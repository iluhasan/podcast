package com.example.podcast;

import org.apache.camel.language.simple.Simple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;

@Component
public class UpdateHelper {
    private final ZonedDateTime startOfTime = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault());

    private ZonedDateTime notOlderThanDate;
    private ZonedDateTime latestPodcastDate;

    @Autowired
    public UpdateHelper(Environment env) {
        Integer notOlderThanDays = Integer.parseInt(env.getProperty("notOlderThanDays"));
        this.notOlderThanDate = ZonedDateTime.now().minusDays(notOlderThanDays);
        this.latestPodcastDate = startOfTime;
    }

    public boolean isSuitablePublication(@Simple("${header.publicationDate}") String publicationDate, @Simple("${header.latestLoadedPodcastDate}") String needNewerThan) {
        try {
            ZonedDateTime currentPodcastDate = ZonedDateTime.parse(publicationDate, DateTimeFormatter.RFC_1123_DATE_TIME);
            ZonedDateTime needNewerThanDate = needNewerThan != null ? ZonedDateTime.parse(needNewerThan, DateTimeFormatter.RFC_1123_DATE_TIME) : startOfTime;

            if (needNewerThanDate.compareTo(this.latestPodcastDate) > 0) {
                this.latestPodcastDate = needNewerThanDate;
            }
            if (currentPodcastDate.compareTo(this.latestPodcastDate) > 0) {
                this.latestPodcastDate = currentPodcastDate;
            }

            return (currentPodcastDate.compareTo(this.notOlderThanDate) >= 0 && currentPodcastDate.compareTo(needNewerThanDate) > 0);
        } catch (DateTimeException exc) {
            System.out.println("Date parsing failed: " + exc.getMessage());
            return true;
        }
    }

    public String getLatestPodcastDate() {
        return this.latestPodcastDate.format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }
}
