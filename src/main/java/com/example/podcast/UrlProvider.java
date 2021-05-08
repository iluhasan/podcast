package com.example.podcast;

import org.springframework.stereotype.Service;

@Service
public class UrlProvider {
    private final String stateFileName = "latestLoadedPodcastDate.txt";

    public String getStateFileInUrl() { return "file:target/rss?fileName=" + stateFileName + "&noop=true&idempotent=false&sendEmptyMessageWhenIdle=true&delay=5m"; }

    public String getStateFileOutUrl() { return "file:target/rss?fileName=" + stateFileName; }

    public String getRssFeedUrl() {
        return "{{rssURL}}";
    }

    public String getDestFolderUrl() {
        return "file:{{destFolder}}";
    }
}
