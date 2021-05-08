package com.example.podcast;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MyRouteBuilder extends RouteBuilder {
    private final String latestLoadedPodcastDateStorage = "latestLoadedPodcastDate.txt";
    private UrlProvider urlProvider;
    private UpdateHelper updateHelper;

    @Autowired
    public MyRouteBuilder(UrlProvider urlProvider, UpdateHelper updateHelper) {
        this.urlProvider = urlProvider;
        this.updateHelper = updateHelper;
    }

    @Override
    public void configure() {
        from(urlProvider.getStateFileInUrl()) // read date of the latest loaded podcast
                .log("Started loading new podcasts since '${body}' from rss: {{rssURL}}")
                .setHeader("latestLoadedPodcastDate", body())
                .to(urlProvider.getRssFeedUrl()) // load RSS feed
                .split().xpath("rss/channel/item").stopOnException()
                    .setHeader("publicationDate", xpath("/item/pubDate/text()"))
                    .filter(method(updateHelper, "isSuitablePublication"))
                        .setHeader(Exchange.FILE_NAME, xpath("/item/guid/text()").append(constant(".mp3")))
                        .setHeader(Exchange.HTTP_QUERY, constant("throwExceptionOnFailure=false")) // to suppress redirection exception (http status 302)
                        .toD("language:xpath:/item/enclosure/@url") // download a podcast
                        .setHeader(Exchange.HTTP_QUERY, constant("throwExceptionOnFailure=true"))
                        .to(urlProvider.getDestFolderUrl()) // save downloaded podcast to destination folder
                        .log("Podcast ${header." + Exchange.FILE_NAME + "} as of ${header.publicationDate} saved to folder {{destFolder}}")
                    .end()
                .end()
                .setBody(method(updateHelper, "getLatestPodcastDate"))
                .to(urlProvider.getStateFileOutUrl()) // save date of the latest loaded podcast
                .log("Done.");
    }
}
