package org.me.gcu.mboue_jonathan_s2028595.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Downloads one Traffic Scotland feed as a String. Blocking: always call it
 * from a background thread. Uses the same Authorization header as the
 * starter code.
 */
public final class FeedClient {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    private FeedClient() {
    }

    public static String download(String address, String clientId, String clientKey)
            throws IOException {

        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Authorization", Base64.getEncoder().encodeToString(
                (clientId + ":" + clientKey).getBytes(StandardCharsets.UTF_8)));

        try {
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + code + " from feed");
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buffer = new char[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    sb.append(buffer, 0, read);
                }
            }
            // Clean up any leading garbage characters before the XML declaration.
            int start = sb.indexOf("<?");
            if (start < 0) {
                start = sb.indexOf("<");
            }
            if (start < 0) {
                throw new IOException("Response was not XML");
            }
            return sb.substring(start);
        } finally {
            connection.disconnect();
        }
    }
}
