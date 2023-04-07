package fr.cnes.regards.modules.acquisition.service;

import org.junit.Assert;
import org.junit.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class CronComparatorTest {

    @Test
    public void shouldReturnTrue() {
        // Cron every 1 min
        String cron = "0 * * * * *";
        // Let's simulate we are less than 1 min before the next CRON date
        OffsetDateTime dateTime = OffsetDateTime.of(2010, 1, 1, 13, 59, 10, 0, ZoneOffset.UTC);
        OffsetDateTime lastCheckDate = dateTime.minusMinutes(1);
        Assert.assertTrue(CronComparator.shouldRun(cron, lastCheckDate, dateTime));

        // 1ms before
        dateTime = OffsetDateTime.of(2010, 1, 1, 13, 59, 59, 9999, ZoneOffset.UTC);
        lastCheckDate = dateTime.minusMinutes(1);
        Assert.assertTrue(CronComparator.shouldRun(cron, lastCheckDate, dateTime));

        // Exactly 1 min before
        dateTime = OffsetDateTime.of(2010, 1, 1, 13, 59, 0, 0, ZoneOffset.UTC);
        lastCheckDate = dateTime.minusMinutes(1);
        Assert.assertTrue(CronComparator.shouldRun(cron, lastCheckDate, dateTime));
    }

    @Test
    public void shouldReturnFalse() {
        // Cron every 30mins
        String cron = "0 30 * * * *";

        // Let's simulate we are more than 1 min before the next CRON date
        OffsetDateTime currentDateTime = OffsetDateTime.of(2010, 1, 1, 13, 28, 59, 0, ZoneOffset.UTC);
        OffsetDateTime lastCheckDate = currentDateTime.minusMinutes(1);
        Assert.assertFalse(CronComparator.shouldRun(cron, lastCheckDate, currentDateTime));

        // Let's simulate we already are in the next occurrence
        currentDateTime = OffsetDateTime.of(2010, 1, 1, 13, 31, 1, 0, ZoneOffset.UTC);
        lastCheckDate = currentDateTime.minusMinutes(1);
        Assert.assertFalse(CronComparator.shouldRun(cron, lastCheckDate, currentDateTime));
    }

}