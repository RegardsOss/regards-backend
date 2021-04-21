package fr.cnes.regards.modules.acquisition.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.Assert;
import org.junit.Test;

public class CronComparatorTest {

    @Test
    public void shouldReturnTrue() {
        // Cron every 1 min
        String cron = "0 * * * * *";
        // Let's simulate we are less than 1 min before the next CRON date
        long currentDateAsLong = OffsetDateTime.of(2010, 1, 1, 13, 59, 10, 0, ZoneOffset.UTC).toEpochSecond() * 1000;
        Assert.assertEquals(true, CronComparator.shouldRun(cron, currentDateAsLong));

        // 1ms before
        currentDateAsLong = OffsetDateTime.of(2010, 1, 1, 13, 59, 59, 9999, ZoneOffset.UTC).toEpochSecond() * 1000;
        Assert.assertEquals(true, CronComparator.shouldRun(cron, currentDateAsLong));

        // Exactly 1 min before
        currentDateAsLong = OffsetDateTime.of(2010, 1, 1, 13, 59, 0, 0, ZoneOffset.UTC).toEpochSecond() * 1000;
        Assert.assertEquals(true, CronComparator.shouldRun(cron, currentDateAsLong));
    }

    @Test
    public void shouldReturnFalse() {
        // Cron every 30mins
        String cron = "0 30 * * * *";

        // Let's simulate we are more than 1 min before the next CRON date
        long currentDateAsLong = OffsetDateTime.of(2010, 1, 1, 13, 28, 59, 0, ZoneOffset.UTC).toEpochSecond() * 1000;
        Assert.assertEquals(false, CronComparator.shouldRun(cron, currentDateAsLong));

        // Let's simulate we already are in the next occurrence
        currentDateAsLong = OffsetDateTime.of(2010, 1, 1, 13, 30, 0, 0, ZoneOffset.UTC).toEpochSecond() * 1000;
        Assert.assertEquals(false, CronComparator.shouldRun(cron, currentDateAsLong));
    }

}