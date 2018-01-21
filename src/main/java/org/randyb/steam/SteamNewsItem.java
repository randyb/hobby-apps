package org.randyb.steam;

public class SteamNewsItem {
    public final SteamGame game;
    public final NewsItem news;

    public SteamNewsItem(SteamGame game, NewsItem news) {
        this.game = game;
        this.news = news;
    }

    @Override
    public String toString() {
        return String.format("%s: %s %s", game.name, news.title, news.url);
    }
}
