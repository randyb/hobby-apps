package org.randyb.utils;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends push notifications to via prowlapp.com
 */
public class ProwlUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ProwlUtil.class);
    private static final String PROWL_KEY = System.getenv("PROWL_KEY");
    private static final String URL = "https://api.prowlapp.com/publicapi/add";
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    public static void sendNotification(String app, String event) throws Exception {
        FormBody body = new FormBody.Builder()
            .add("apikey", PROWL_KEY)
            .add("application", app)
            .add("event", event)
            .build();
        Request request = new Request.Builder()
            .url(URL)
            .post(body)
            .build();

        String response = HTTP_CLIENT.newCall(request).execute().body().string();

        LOG.info("Prowl response: {}", response);
    }
}
