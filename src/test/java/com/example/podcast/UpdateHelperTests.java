package com.example.podcast;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;


public class UpdateHelperTests {

    private static Environment env;

    private static String startOfTime;
    private static String today;
    private static String minus2days;
    private static String minus5days;

    @BeforeAll
    public static void beforeAllTestMethods() {
        env = Mockito.mock(Environment.class);
        Mockito.when(env.getProperty("notOlderThanDays")).thenReturn("3");

        startOfTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault()).format(DateTimeFormatter.RFC_1123_DATE_TIME);
        today = ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
        minus2days = ZonedDateTime.now().minusDays(2).format(DateTimeFormatter.RFC_1123_DATE_TIME);
        minus5days = ZonedDateTime.now().minusDays(5).format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    @Test
    void publication_shouldBeSuitable_ifNoLatestLoadedPodcastDateAndPublicationIsNotTooOld() {
        String publicationDate = today;
        String latestLoadedPodcastDate = null;

        UpdateHelper updateHelper = new UpdateHelper(this.env);
        Boolean result = updateHelper.isSuitablePublication(publicationDate, latestLoadedPodcastDate);
        String latestPodcastDate = updateHelper.getLatestPodcastDate();

        assertThat(result).isTrue();
        assertThat(latestPodcastDate).isEqualTo(publicationDate);
    }

    @Test
    void publication_shouldNotBeSuitable_ifItIsTooOld() {
        String publicationDate = minus5days;
        String latestLoadedPodcastDate = null;

        UpdateHelper updateHelper = new UpdateHelper(this.env);
        Boolean result = updateHelper.isSuitablePublication(publicationDate, latestLoadedPodcastDate);
        String latestPodcastDate = updateHelper.getLatestPodcastDate();

        assertThat(result).isFalse();
        assertThat(latestPodcastDate).isEqualTo(publicationDate);
    }

    @Test
    void publication_shouldNotBeSuitable_ifItIsOlderThanLatestLoadedPodcast() {
        String publicationDate = minus2days;
        String latestLoadedPodcastDate = today;

        UpdateHelper updateHelper = new UpdateHelper(this.env);
        Boolean result = updateHelper.isSuitablePublication(publicationDate, latestLoadedPodcastDate);
        String latestPodcastDate = updateHelper.getLatestPodcastDate();

        assertThat(result).isFalse();
        assertThat(latestPodcastDate).isEqualTo(latestLoadedPodcastDate);
    }

    @Test
    void publication_shouldBeSuitable_ifItIsNewerThanLatestLoadedPodcast() {
        String publicationDate = today;
        String latestLoadedPodcastDate = minus2days;

        UpdateHelper updateHelper = new UpdateHelper(this.env);
        Boolean result = updateHelper.isSuitablePublication(publicationDate, latestLoadedPodcastDate);
        String latestPodcastDate = updateHelper.getLatestPodcastDate();

        assertThat(result).isTrue();
        assertThat(latestPodcastDate).isEqualTo(publicationDate);
    }

    @Test
    void publication_shouldBeConsideredSuitable_ifWrongDateFormat() {
        String publicationDate = "wrong_date";
        String latestLoadedPodcastDate = today;

        UpdateHelper updateHelper = new UpdateHelper(this.env);
        Boolean result = updateHelper.isSuitablePublication(publicationDate, latestLoadedPodcastDate);
        String latestPodcastDate = updateHelper.getLatestPodcastDate();

        assertThat(result).isTrue();
        assertThat(latestPodcastDate).isEqualTo(startOfTime);
    }
}


