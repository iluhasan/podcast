package com.example.podcast;

import org.apache.camel.*;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static com.example.podcast.PodcastTestConfig.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(CamelSpringBootRunner.class)
@SpringBootTest(classes = { PodcastTestConfig.class }, properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "notOlderThanDays=4"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MyRouteBuilderTests {
    @Autowired
    private CamelContext context;

    @Autowired
    private ProducerTemplate template;

    @EndpointInject(RssFeedMockUrl)
    private MockEndpoint mockRss;

    @EndpointInject(StateFileOutMockUrl)
    protected MockEndpoint mockStateFile;

    @EndpointInject(DestFolderMockUrl)
    protected MockEndpoint mockDestFolder;

    @EndpointInject(Podcast1MockUrl)
    protected MockEndpoint mockPodcast1;
    @EndpointInject(Podcast2MockUrl)
    protected MockEndpoint mockPodcast2;
    @EndpointInject(Podcast3MockUrl)
    protected MockEndpoint mockPodcast3;

    private String today;
    private String minus2days;
    private String minus3days;

    @BeforeEach
    void setUp()  throws IOException {
        today = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        minus2days = ZonedDateTime.now().minusDays(2).format(DateTimeFormatter.RFC_1123_DATE_TIME);
        minus3days = ZonedDateTime.now().minusDays(3).format(DateTimeFormatter.RFC_1123_DATE_TIME);
        String minus5days = ZonedDateTime.now().minusDays(5).format(DateTimeFormatter.RFC_1123_DATE_TIME);

        Path testRssTemplatePath = Paths.get("src","test","resources","TestRssFeed.xml");
        String testRssTemplate = new String(Files.readAllBytes(testRssTemplatePath), StandardCharsets.UTF_8);
        String testRssFeed = testRssTemplate
                .replace("%PUBDATE1%", today)
                .replace("%PUBDATE2%", minus2days)
                .replace("%PUBDATE3%", minus5days)
                .replace("%PODCAST1%", Podcast1MockUrl)
                .replace("%PODCAST2%", Podcast2MockUrl)
                .replace("%PODCAST3%", Podcast3MockUrl);
        mockRss.whenAnyExchangeReceived(e -> e.getIn().setBody(testRssFeed));
    }

    @Test
    public void podcasts_newerThanLatestLoadedOne_shouldBeLoaded() throws Exception {
        assertThat(context.getStatus()).isEqualTo(ServiceStatus.Started);

        // podcasts not older than 3 days should be loaded
        String latestLoadedPodcastDate = minus3days;
        template.sendBody(PodcastTestConfig.StateFileInMockUrl, latestLoadedPodcastDate);

        // rss feed fetched
        mockRss.expectedMessageCount(1);
        mockRss.expectedHeaderReceived("latestLoadedPodcastDate", latestLoadedPodcastDate);

        // podcast from the 1st item in the test RSS feed should be loaded as it's newer than the latest loaded podcast
        mockPodcast1.expectedMessageCount(1);
        mockPodcast1.expectedHeaderReceived("publicationDate", today);
        mockPodcast1.whenAnyExchangeReceived(e -> e.getIn().setBody(123));

        // podcast from the 2nd item in the test RSS feed should be loaded as it's newer than the latest loaded podcast
        mockPodcast2.expectedMessageCount(1);
        mockPodcast2.expectedHeaderReceived("publicationDate", minus2days);
        mockPodcast2.whenAnyExchangeReceived(e -> e.getIn().setBody(456));

        // 3rd item in the test RSS feed should be filtered out as it's older than the latest loaded podcast
        mockPodcast3.expectedMessageCount(0);

        // two audio files are saved to dest folder
        mockDestFolder.expectedMessageCount(2);
        mockDestFolder.message(0).header(Exchange.FILE_NAME).isEqualTo("podcast1.mp3");
        mockDestFolder.message(1).header(Exchange.FILE_NAME).isEqualTo("podcast2.mp3");
        mockDestFolder.message(0).body().isEqualTo(123);
        mockDestFolder.message(1).body().isEqualTo(456);

        mockStateFile.expectedMessageCount(1);
        mockStateFile.message(0).body().isEqualTo(today); // publication date of the latest stored podcast

        NotifyBuilder notify = new NotifyBuilder(context)
                .wereSentTo(PodcastTestConfig.StateFileOutMockUrl)
                .whenDone(1)
                .create();

        assertTrue(notify.matchesWaitTime());
        mockRss.assertIsSatisfied();
        mockPodcast1.assertIsSatisfied();
        mockPodcast2.assertIsSatisfied();
        mockPodcast3.assertIsSatisfied();
        mockDestFolder.assertIsSatisfied();
        mockStateFile.assertIsSatisfied();
    }

    @Test
    public void podcasts_shouldNotBeLoaded_ifNewestPodcastHasAlreadyBeenLoaded() throws Exception {
        assertThat(context.getStatus()).isEqualTo(ServiceStatus.Started);

        // all podcasts have been loaded as of now
        String latestLoadedPodcastDate = today;
        template.sendBody(PodcastTestConfig.StateFileInMockUrl, latestLoadedPodcastDate);

        // rss feed fetched
        mockRss.expectedMessageCount(1);
        mockRss.expectedHeaderReceived("latestLoadedPodcastDate", latestLoadedPodcastDate);

        // all items in the test RSS feed should be filtered out as they have already been loaded
        mockPodcast1.expectedMessageCount(0);
        mockPodcast2.expectedMessageCount(0);
        mockPodcast3.expectedMessageCount(0);

        // nothing saved to dest folder
        mockDestFolder.expectedMessageCount(0);

        mockStateFile.expectedMessageCount(1);
        mockStateFile.message(0).body().isEqualTo(today); // the same latestLoadedPodcastDate is stored

        NotifyBuilder notify = new NotifyBuilder(context)
                .wereSentTo(PodcastTestConfig.StateFileOutMockUrl)
                .whenDone(1)
                .create();

        assertTrue(notify.matchesWaitTime());
        mockRss.assertIsSatisfied();
        mockPodcast1.assertIsSatisfied();
        mockPodcast2.assertIsSatisfied();
        mockPodcast3.assertIsSatisfied();
        mockDestFolder.assertIsSatisfied();
        mockStateFile.assertIsSatisfied();
    }

    @Test
    // The application property notOlderThanDays=4 for the test defines lower bound for podcasts
    public void podcastsNotOlderThan4Days_shouldBeLoaded_ifNoStateFile() throws Exception {
        assertThat(context.getStatus()).isEqualTo(ServiceStatus.Started);

        // no state file
        template.sendBody(PodcastTestConfig.StateFileInMockUrl, null);

        // rss feed fetched
        mockRss.expectedMessageCount(1);
        mockRss.expectedHeaderReceived("latestLoadedPodcastDate", null);

        // podcast from the 1st item in the test RSS feed should be loaded as it's not older than 4 days
        mockPodcast1.expectedMessageCount(1);
        mockPodcast1.expectedHeaderReceived("publicationDate", today);
        mockPodcast1.whenAnyExchangeReceived(e -> e.getIn().setBody(123));

        // podcast from the 2nd item in the test RSS feed should be loaded as it's not older than 4 days
        mockPodcast2.expectedMessageCount(1);
        mockPodcast2.expectedHeaderReceived("publicationDate", minus2days);
        mockPodcast2.whenAnyExchangeReceived(e -> e.getIn().setBody(456));

        // 3rd item in the test RSS feed should be filtered out as it's older than 4 days
        mockPodcast3.expectedMessageCount(0);

        // two audio files are saved to dest folder
        mockDestFolder.expectedMessageCount(2);
        mockDestFolder.message(0).header(Exchange.FILE_NAME).isEqualTo("podcast1.mp3");
        mockDestFolder.message(1).header(Exchange.FILE_NAME).isEqualTo("podcast2.mp3");
        mockDestFolder.message(0).body().isEqualTo(123);
        mockDestFolder.message(1).body().isEqualTo(456);

        mockStateFile.expectedMessageCount(1);
        mockStateFile.message(0).body().isEqualTo(today); // publication date of the latest stored podcast

        NotifyBuilder notify = new NotifyBuilder(context)
                .wereSentTo(PodcastTestConfig.StateFileOutMockUrl)
                .whenDone(1)
                .create();

        assertTrue(notify.matchesWaitTime());
        mockRss.assertIsSatisfied();
        mockPodcast1.assertIsSatisfied();
        mockPodcast2.assertIsSatisfied();
        mockPodcast3.assertIsSatisfied();
        mockDestFolder.assertIsSatisfied();
        mockStateFile.assertIsSatisfied();
    }

    @Test
    public void podcasts_shouldNotBeLoaded_ifLoadOfRssFeedFailed() throws Exception {
        assertThat(context.getStatus()).isEqualTo(ServiceStatus.Started);

        // loading of RSS feed fails
        mockRss.whenAnyExchangeReceived(e -> { throw new ConnectException("Simulated connection error"); });

        // podcasts not older than 3 days should be loaded
        String latestLoadedPodcastDate = minus3days;

        // rss feed requested
        mockRss.expectedMessageCount(1);
        mockRss.expectedHeaderReceived("latestLoadedPodcastDate", latestLoadedPodcastDate);

        // no podcasts loaded
        mockPodcast1.expectedMessageCount(0);
        mockPodcast2.expectedMessageCount(0);
        mockPodcast3.expectedMessageCount(0);

        // nothing saved to dest folder
        mockDestFolder.expectedMessageCount(0);

        // the state file is not updated
        mockStateFile.expectedMessageCount(0);

        template.sendBody(PodcastTestConfig.StateFileInMockUrl, latestLoadedPodcastDate);
        NotifyBuilder notify = new NotifyBuilder(context)
                .whenFailed(1)
                .create();

        assertTrue(notify.matchesWaitTime());
        mockRss.assertIsSatisfied();
        mockPodcast1.assertIsSatisfied();
        mockPodcast2.assertIsSatisfied();
        mockPodcast3.assertIsSatisfied();
        mockDestFolder.assertIsSatisfied();
        mockStateFile.assertIsSatisfied();
    }
}

