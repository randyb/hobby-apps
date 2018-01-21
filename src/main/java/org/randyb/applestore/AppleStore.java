package org.randyb.applestore;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.randyb.utils.ProwlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Status;
import twitter4j.TwitterFactory;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static j2html.TagCreator.*;
import static spark.Spark.get;
import static spark.Spark.port;

public class AppleStore {

    private static final Logger LOG = LoggerFactory.getLogger(AppleStore.class);

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    private static final String base_url = "https://www.apple.com/shop/retail/pickup-message?pl=true&cppart=ATT/US&location=75248&parts.0=";
    private static final String iphone8 = "MQ6V2LL/A";
    private static final String iphoneX_64gb = "MQAJ2LL/A";
    private static final String iphoneX_256gb = "MQAM2LL/A";

    private static final String URL = base_url + iphoneX_256gb;

    private static Map<String, Boolean> STOCK = Maps.newConcurrentMap();

    public static void addServices() {

//        new ScheduledThreadPoolExecutor(1)
//            .scheduleWithFixedDelay(()-> {
//                checkStores(URL);
//            }, 0, 1, TimeUnit.MINUTES);

        get("/applestore", (req, res) -> {
            List<Store> stores = getStores(URL);
            return html(
                head(title("Apple Store Stock")),
                body(
                    div(
                        table(
                            each(stores, store -> {
                                Part part = store.partsAvailability.values().iterator().next();
                                return tr(
                                    td(store.storeName),
                                    td(part.storePickupProductTitle),
                                    td(part.pickupDisplay)
                                ).withCondStyle(part.pickupDisplay.equals("available"), "background-color: lightgreen");
                            })
                        )
                    )
                )
            ).renderFormatted();

        });
    }

    private static void checkStores(String url) {
        LOG.info("Checking iphone stock");
        try {
            List<Store> stores = getStores(url);
            List<Store> nearbyStores = stores.stream().filter(s -> s.storedistance < 15.0).collect(Collectors.toList());

            for (Store s : nearbyStores) {
                String storeName = s.storeName;
                Boolean newStock = s.partsAvailability.values().iterator().next().pickupDisplay.equals("available");

                Boolean current = STOCK.put(storeName, newStock);
                if (current != null && current != newStock) {
                    if (newStock) {
                        tweet(storeName + " now has stock");
                        ProwlUtil.sendNotification("AppleStore", storeName + " now has stock");
                    } else {
                        tweet(storeName + " out of stock");
                        ProwlUtil.sendNotification("AppleStore", storeName + " out of stock");
                    }
                }
            }

        } catch (Exception e) {
            LOG.error("Unable to query apple store", e);
        }
    }

    private static void tweet(String msg) throws Exception {
        Status status = TwitterFactory.getSingleton().updateStatus(msg + " " + new java.util.Date());
        LOG.info("Tweeted: {}", status);
        Thread.sleep(5000);
    }

    private static List<Store> getStores(String url) throws Exception {
        Request request = new Request.Builder().url(url).build();
        String jsonStr = HTTP_CLIENT.newCall(request).execute().body().string();
        JsonElement json = new JsonParser().parse(jsonStr);
        JsonArray storesArray = json.getAsJsonObject().getAsJsonObject("body").getAsJsonArray("stores");
        Type type = new TypeToken<List<Store>>() {}.getType();
        return new Gson().fromJson(storesArray, type);
    }

    private static class Store {
        public String storeName;
        public double storedistance;
        public Map<String, Part> partsAvailability;
    }

    private static class Part {
        public String storePickupProductTitle;
        public String pickupDisplay;
    }

    public static void main(String[] args) throws Exception {
        port(80);
        addServices();

        //Prowl.sendNotification("AppleStore", "this is a test message");
    }

}
