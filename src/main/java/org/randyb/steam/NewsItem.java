package org.randyb.steam;

public class NewsItem {
    public String gid;
    public String title;
    public String url;

    public NewsItem(String gid, String title, String contents, String url) {
        this.gid = gid;
        this.title = title;
        this.url = url;
    }
}
