package org.me.gcu.mboue_jonathan_s2028595.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Reads the Traffic Scotland credentials from AndroidManifest meta-data.
 * The manifest values are filled in at build time from local.properties
 * (see protectingYourKeys.docx), so no key is stored in the source code.
 */
public final class ApiConfig {
    private static final String TAG = "ApiConfig";

    public final String clientId;
    public final String clientKey;
    public final String roadworksUrl;

    private ApiConfig(String clientId, String clientKey, String roadworksUrl) {
        this.clientId = clientId;
        this.clientKey = clientKey;
        this.roadworksUrl = roadworksUrl;
    }

    public static ApiConfig load(Context context) {
        String id = "";
        String key = "";
        String roadworks = "";
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle meta = info.metaData;
            if (meta != null) {
                id = valueOf(meta.get("TS_CLIENT_ID"));
                key = valueOf(meta.get("TS_CLIENT_KEY"));
                roadworks = valueOf(meta.get("TS_ROADWORKS_URL"));
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot read manifest meta-data", e);
        }
        return new ApiConfig(id, key, roadworks);
    }

    /** Numeric-looking values such as a client ID can arrive as Integer, so convert safely. */
    private static String valueOf(Object o) {
        return o == null ? "" : o.toString().trim();
    }
}
