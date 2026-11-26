package org.me.gcu.mboue_jonathan_s2028595.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.mboue_jonathan_s2028595.data.SituationFilter;
import org.me.gcu.mboue_jonathan_s2028595.data.TrafficRepository;
import org.me.gcu.mboue_jonathan_s2028595.model.Situation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The ViewModel of the MVVM design. It survives screen rotation, so the lists,
 * the selected situation, the search results and the map selection all stay
 * as they were when the layout changes between portrait and landscape.
 *
 * Model:      Situation classes + TrafficRepository
 * ViewModel:  this class
 * View:       the activity and fragments in the ui package
 */
public class TrafficViewModel extends AndroidViewModel {

    public static final int DEFAULT_REFRESH_MINUTES = 60;

    private final TrafficRepository repository;

    private final MutableLiveData<Situation> selected = new MutableLiveData<>();
    private final MutableLiveData<List<Situation>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<String> searchSummary = new MutableLiveData<>();
    private final MutableLiveData<List<Situation>> mapSubset = new MutableLiveData<>();
    private final MutableLiveData<double[]> mapCentre = new MutableLiveData<>();

    /** {lat, lon, radiusKm} of the last distance search, or null. */
    private double[] searchCentre;
    private boolean lastSearchByDistance;
    private int refreshMinutes = DEFAULT_REFRESH_MINUTES;

    public TrafficViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);
        repository.startAutoUpdate(refreshMinutes); // fetches at start-up, then repeats
    }

    // ---- data ------------------------------------------------------------

    public LiveData<List<Situation>> getIncidents() {
        return repository.getIncidents();
    }

    public LiveData<List<Situation>> getRoadworks() {
        return repository.getRoadworks();
    }

    public LiveData<TrafficRepository.FeedState> getFeedState() {
        return repository.getFeedState();
    }

    public List<Situation> getAllSituations() {
        List<Situation> all = new ArrayList<>();
        List<Situation> inc = repository.getIncidents().getValue();
        List<Situation> rw = repository.getRoadworks().getValue();
        if (inc != null) {
            all.addAll(inc);
        }
        if (rw != null) {
            all.addAll(rw);
        }
        return all;
    }

    public List<String> getKnownRoads() {
        return SituationFilter.knownRoads(getAllSituations());
    }

    // ---- refresh ---------------------------------------------------------

    public void refresh() {
        repository.refreshNow();
    }

    public int getRefreshMinutes() {
        return refreshMinutes;
    }

    public void setRefreshMinutes(int minutes) {
        refreshMinutes = minutes;
        repository.startAutoUpdate(minutes);
    }

    // ---- selection -------------------------------------------------------

    public LiveData<Situation> getSelected() {
        return selected;
    }

    public void select(Situation s) {
        selected.setValue(s);
    }

    // ---- search ----------------------------------------------------------

    public LiveData<List<Situation>> getSearchResults() {
        return searchResults;
    }

    public LiveData<String> getSearchSummary() {
        return searchSummary;
    }

    public boolean isLastSearchByDistance() {
        return lastSearchByDistance;
    }

    public void searchByRoad(String query, int scope) {
        lastSearchByDistance = false;
        searchCentre = null;
        List<Situation> results = SituationFilter.byRoad(getAllSituations(), query, scope);
        searchResults.setValue(results);
        searchSummary.setValue(results.size() + " result" + (results.size() == 1 ? "" : "s")
                + " for road \"" + query.trim() + "\"");
    }

    public void searchByDistance(double lat, double lon, double radiusKm, int scope,
                                 String locationLabel) {
        lastSearchByDistance = true;
        searchCentre = new double[]{lat, lon, radiusKm};
        List<Situation> results =
                SituationFilter.byDistance(getAllSituations(), lat, lon, radiusKm, scope);
        searchResults.setValue(results);
        searchSummary.setValue(results.size() + " within " + (int) radiusKm + " km of "
                + locationLabel);
    }

    // ---- map -------------------------------------------------------------

    public LiveData<List<Situation>> getMapSubset() {
        return mapSubset;
    }

    /** {lat, lon, radiusKm} to draw as a search circle on the map, or null. */
    public LiveData<double[]> getMapCentre() {
        return mapCentre;
    }

    /** Show one selected situation on the map. */
    public void showSituationOnMap(Situation s) {
        mapCentre.setValue(null);
        mapSubset.setValue(new ArrayList<>(Collections.singletonList(s)));
    }

    /** Show the current search results (several situations) on the map. */
    public void showSearchResultsOnMap() {
        List<Situation> results = searchResults.getValue();
        if (results == null) {
            return;
        }
        mapCentre.setValue(lastSearchByDistance ? searchCentre : null);
        mapSubset.setValue(new ArrayList<>(results));
    }

    /** Back to the map of every situation. */
    public void showAllOnMap() {
        mapCentre.setValue(null);
        mapSubset.setValue(null);
    }

    @Override
    protected void onCleared() {
        repository.shutdown();
    }
}
