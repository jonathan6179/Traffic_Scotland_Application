package org.me.gcu.mboue_jonathan_s2028595.data;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.mboue_jonathan_s2028595.model.Situation;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Single source of truth for the traffic data.
 *
 * Threading: one background thread (a ScheduledExecutorService) does all the
 * downloading and parsing. Results reach the UI through LiveData.postValue(),
 * so there is no runOnUiThread and no AsyncTask. The same thread runs the
 * start-up fetch, the periodic auto-update and the manual refresh, one after
 * another, so two downloads can never overlap.
 *
 * Offline behaviour: every successful download is cached to a file. If a
 * download fails, the cached copy is parsed instead and the state becomes
 * OFFLINE_CACHE.
 */
public class TrafficRepository {
    private static final String TAG = "TrafficRepository";

    public static final String UNPLANNED_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    private static final String CACHE_INCIDENTS = "cache_unplanned_events.xml";
    private static final String CACHE_ROADWORKS = "cache_roadworks.xml";

    public enum Status {LOADING, LIVE, OFFLINE_CACHE, ERROR}

    /** What the status banner shows. */
    public static final class FeedState {
        public final Status status;
        public final long dataTimeMillis;
        public final String message;

        FeedState(Status status, long dataTimeMillis, String message) {
            this.status = status;
            this.dataTimeMillis = dataTimeMillis;
            this.message = message;
        }
    }

    private final Context appContext;
    private final ApiConfig config;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> periodicTask;
    private boolean started;
    private long lastDataTime;

    private final MutableLiveData<List<Situation>> incidents =
            new MutableLiveData<>(Collections.<Situation>emptyList());
    private final MutableLiveData<List<Situation>> roadworks =
            new MutableLiveData<>(Collections.<Situation>emptyList());
    private final MutableLiveData<FeedState> feedState =
            new MutableLiveData<>(new FeedState(Status.LOADING, 0, "Starting"));

    public TrafficRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.config = ApiConfig.load(appContext);
    }

    public LiveData<List<Situation>> getIncidents() {
        return incidents;
    }

    public LiveData<List<Situation>> getRoadworks() {
        return roadworks;
    }

    public LiveData<FeedState> getFeedState() {
        return feedState;
    }

    /**
     * Fetches now (start-up update) and then every {@code minutes} minutes.
     * Calling it again with a new interval restarts the timer without an extra fetch.
     */
    public synchronized void startAutoUpdate(int minutes) {
        if (periodicTask != null) {
            periodicTask.cancel(false);
        }
        long initialDelay = started ? minutes : 0;
        started = true;
        periodicTask = scheduler.scheduleWithFixedDelay(
                this::refreshBlocking, initialDelay, minutes, TimeUnit.MINUTES);
    }

    /** Manual refresh. Runs on the same background thread. */
    public void refreshNow() {
        scheduler.execute(this::refreshBlocking);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    // ---- background work -------------------------------------------------

    private void refreshBlocking() {
        try {
            feedState.postValue(new FeedState(Status.LOADING, lastDataTime, "Updating"));

            long[] cacheTimes = new long[2];
            List<Situation> newIncidents = loadFeed(
                    UNPLANNED_URL, CACHE_INCIDENTS, false, cacheTimes, 0);
            List<Situation> newRoadworks = config.roadworksUrl.isEmpty() ? null
                    : loadFeed(config.roadworksUrl, CACHE_ROADWORKS, true, cacheTimes, 1);

            if (newIncidents == null && newRoadworks == null) {
                String why = config.clientId.isEmpty() || config.roadworksUrl.isEmpty()
                        ? "Credentials or roadworks URL missing in local.properties"
                        : "No connection and no saved data";
                feedState.postValue(new FeedState(Status.ERROR, lastDataTime, why));
                return;
            }
            if (newIncidents != null) {
                incidents.postValue(Collections.unmodifiableList(newIncidents));
            }
            if (newRoadworks != null) {
                roadworks.postValue(Collections.unmodifiableList(newRoadworks));
            }

            long oldestCache = 0;
            for (long t : cacheTimes) {
                if (t > 0 && (oldestCache == 0 || t < oldestCache)) {
                    oldestCache = t;
                }
            }
            if (oldestCache > 0) {
                lastDataTime = oldestCache;
                feedState.postValue(new FeedState(Status.OFFLINE_CACHE, oldestCache,
                        "Offline, showing saved data"));
            } else {
                lastDataTime = System.currentTimeMillis();
                feedState.postValue(new FeedState(Status.LIVE, lastDataTime, "Live data"));
            }
        } catch (Throwable t) {
            // A thrown exception would silently cancel all later scheduled runs.
            Log.e(TAG, "Refresh failed", t);
            feedState.postValue(new FeedState(Status.ERROR, lastDataTime, "Update failed"));
        }
    }

    /**
     * Downloads and parses one feed. On any failure falls back to the cached copy.
     *
     * @param cacheTimes receives the cache file's age when the cached copy was used
     * @return the situations, or null if neither the network nor the cache worked
     */
    private List<Situation> loadFeed(String url, String cacheName, boolean isRoadworks,
                                     long[] cacheTimes, int slot) {
        try {
            String xml = FeedClient.download(url, config.clientId, config.clientKey);
            List<Situation> parsed = SituationParser.parse(xml, isRoadworks);
            writeCache(cacheName, xml);
            return parsed;
        } catch (IOException | XmlPullParserException e) {
            Log.w(TAG, "Download or parse failed for " + cacheName + ": " + e);
        }

        File cacheFile = new File(appContext.getFilesDir(), cacheName);
        if (cacheFile.exists()) {
            try {
                List<Situation> parsed = SituationParser.parse(readCache(cacheFile), isRoadworks);
                cacheTimes[slot] = cacheFile.lastModified();
                return parsed;
            } catch (IOException | XmlPullParserException e) {
                Log.w(TAG, "Cached copy unusable for " + cacheName + ": " + e);
            }
        }
        return null;
    }

    private void writeCache(String name, String xml) {
        try (OutputStream out = new FileOutputStream(new File(appContext.getFilesDir(), name))) {
            out.write(xml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.w(TAG, "Could not write cache " + name, e);
        }
    }

    private String readCache(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[16384];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
