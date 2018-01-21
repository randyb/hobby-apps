package org.randyb.steam;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Status;
import twitter4j.TwitterFactory;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static spark.Spark.get;

/**
 * Scrapes my Steam 'Followed Games' page, gets the latest news for those
 * games, and tweets it at https://twitter.com/randy_steam_bot
 */
public class SteamTwitterBot {
    private static final Logger LOG = LoggerFactory.getLogger(SteamTwitterBot.class);
    private static final String STEAM_USER = System.getenv("STEAM_USER");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Cache<String, SteamNewsItem> CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS)
            .removalListener(n -> LOG.info("Removed from cache: {}", n))
            .build();
    private static boolean firstRun = true;

    public static void addServices() {
        get("/steam/news", (request, response) -> {
            response.type("application/json");
            return ImmutableMap.of(
                    "count", CACHE.size(),
                    "values", CACHE.asMap().values());
        }, GSON::toJson);

        new ScheduledThreadPoolExecutor(1)
                .scheduleWithFixedDelay(SteamTwitterBot::checkForNews,
                        0, 10, TimeUnit.MINUTES);
    }

    private static void checkForNews() {
        try {
            if (firstRun) {
                tweet("Starting up at " + new Date());
            }

            for (SteamGame game : getFollowedGames(STEAM_USER)) {
                for (SteamNewsItem news : getNewsForGame(game)) {
                    boolean isNew = CACHE.asMap().putIfAbsent(news.news.gid, news) == null;
                    if (!firstRun && isNew) {
                        tweet(news.toString());
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error checking for news", e);
        }

        firstRun = false;
    }

    private static void tweet(String msg) throws Exception {
        Status status = TwitterFactory.getSingleton().updateStatus(msg);
        LOG.info("Tweeted: {}", status);
    }

    private static List<SteamGame> getFollowedGames(String profile) throws Exception {
        URL profileUrl = new URL(String.format("http://steamcommunity.com/id/%s/followedgames", profile));
        LOG.debug("Scraping {}", profileUrl);

        Document doc = Jsoup.parse(profileUrl, 0);
        Elements rows = doc.getElementsByAttribute("data-appid");

        List<SteamGame> followedGames = new ArrayList<>();
        for (Element row : rows) {
            String appId = row.attr("data-appid");
            String name = row.getElementsByClass("gameListRowItemName").text();
            followedGames.add(new SteamGame(name, appId));
        }

        return followedGames;
    }

    private static List<SteamNewsItem> getNewsForGame(SteamGame app) throws Exception {
        URL newsUrl = new URL(String.format("http://api.steampowered.com/ISteamNews/GetNewsForApp/v0002/" +
                "?format=json&appid=%s&feeds=steam_community_announcements&maxlength=1&count=1", app.appId));
        LOG.debug("Fetching news for {}", app.name);

        try (Reader reader = new InputStreamReader(newsUrl.openConnection().getInputStream())) {
            JsonArray newsItems = new JsonParser()
                    .parse(reader)
                    .getAsJsonObject()
                    .getAsJsonObject("appnews")
                    .getAsJsonArray("newsitems");

            return StreamSupport.stream(newsItems.spliterator(), false)
                    .map(j -> GSON.fromJson(j, NewsItem.class))
                    .map(j -> new SteamNewsItem(app, j))
                    .collect(Collectors.toList());
        }
    }
}
