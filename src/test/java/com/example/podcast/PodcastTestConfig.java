package com.example.podcast;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class PodcastTestConfig {
    public static final String StateFileInMockUrl = "stub:file:stateFile";
    public static final String StateFileOutMockUrl = "mock:stateFile";
    public static final String RssFeedMockUrl = "mock:rssFeed";
    public static final String DestFolderMockUrl = "mock:destFolder";

    public static final String Podcast1MockUrl = "mock:podcast1";
    public static final String Podcast2MockUrl = "mock:podcast2";
    public static final String Podcast3MockUrl = "mock:podcast3";

    @Bean
    public UrlProvider urlProvider() {
        return new UrlProvider(){
            @Override
            public String getStateFileInUrl() {
                return StateFileInMockUrl;
            }

            @Override
            public String getStateFileOutUrl() {
                return StateFileOutMockUrl;
            }

            @Override
            public String getRssFeedUrl() {
                return RssFeedMockUrl;
            }

            @Override
            public String getDestFolderUrl() {
                return DestFolderMockUrl;
            }
        };
    }
}
