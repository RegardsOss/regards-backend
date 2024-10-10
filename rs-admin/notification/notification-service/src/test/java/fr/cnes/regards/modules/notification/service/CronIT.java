/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.notification.service;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.Calendar;

/**
 * Test class for Cron
 *
 * @author xbrochar
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { CronTestConfiguration.class })
@ActiveProfiles("test")
public class CronIT {

    /**
     * Current year
     */
    private static final int TWO_THOUSAND_AND_SIXTEEN = 2016;

    /**
     * 7
     */
    private static final int SEVEN = 7;

    /**
     * 11
     */
    private static final int ELEVEN = 11;

    /**
     * 12
     */
    private static final int TWELVE = 12;

    /**
     * 17
     */
    private static final int SEVENTEEN = 17;

    /**
     * 26
     */
    private static final int TWENTY_SIX = 26;

    /**
     * Daily cron
     */
    @Value("${regards.notification.cron.daily}")
    private String dailyCron;

    /**
     * Weekly cron
     */
    @Value("${regards.notification.cron.weekly}")
    private String weeklyCron;

    /**
     * Monthly cron
     */
    @Value("${regards.notification.cron.monthly}")
    private String monthlyCron;

    /**
     * Check that the system triggers the daily cron at the right moment when starting before 12h00.
     */
    @Test
    @Requirement("REGARDS_DSL_STO_CMD_140")
    @Purpose("Check that the system triggers the daily cron at the right moment when starting before 12h00.")
    public void testDailyCronStartStrictlyBefore12() {
        final CronTrigger trigger = new CronTrigger(dailyCron);
        // Prepare a start instant
        final Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(TWO_THOUSAND_AND_SIXTEEN, Calendar.OCTOBER, TWENTY_SIX, ELEVEN, 0, 0);
        final Instant startDate = startCalendar.toInstant();

        // Prepare next execution
        final Instant actualDate = getNextExecutionTime(trigger, startDate);
        final Calendar actualCalendar = Calendar.getInstance();
        actualCalendar.setTimeInMillis(actualDate.toEpochMilli());

        // Check that the next execution happened SAME day
        Assert.assertEquals(startCalendar.get(Calendar.DATE), actualCalendar.get(Calendar.DATE));
        // At 12h00
        Assert.assertEquals(TWELVE, actualCalendar.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, actualCalendar.get(Calendar.MINUTE));
        Assert.assertEquals(0, actualCalendar.get(Calendar.SECOND));
    }

    /**
     * Check that the system triggers the daily cron at the right moment when starting at 12h00.
     */
    @Test
    @Requirement("REGARDS_DSL_STO_CMD_140")
    @Purpose("Check that the system triggers the daily cron at the right moment when starting at 12h00.")
    public void testDailyCronStartEqual12() {
        final CronTrigger trigger = new CronTrigger(dailyCron);
        // Prepare a start instant
        final Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(TWO_THOUSAND_AND_SIXTEEN, Calendar.OCTOBER, TWENTY_SIX, TWELVE, 0, 0);
        final Instant startDate = startCalendar.toInstant();

        // Prepare next execution
        final Instant actualDate = getNextExecutionTime(trigger, startDate);
        final Calendar actualCalendar = Calendar.getInstance();
        actualCalendar.setTimeInMillis(actualDate.toEpochMilli());

        // Check that the next execution happened NEXT day
        Assert.assertEquals(startCalendar.get(Calendar.DATE) + 1, actualCalendar.get(Calendar.DATE));
        // At 12h00
        Assert.assertEquals(TWELVE, actualCalendar.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, actualCalendar.get(Calendar.MINUTE));
        Assert.assertEquals(0, actualCalendar.get(Calendar.SECOND));
    }

    /**
     * Check that the system triggers the daily cron at the right moment when starting after 12h00.
     */
    @Test
    @Requirement("REGARDS_DSL_STO_CMD_140")
    @Purpose("Check that the system triggers the daily cron at the right moment when starting after 12h00.")
    public void testDailyCronStartStrictlyAfter12() {
        final CronTrigger trigger = new CronTrigger(dailyCron);
        // Prepare a start instant
        final Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(TWO_THOUSAND_AND_SIXTEEN, Calendar.OCTOBER, TWENTY_SIX, SEVENTEEN, 0, 0);
        final Instant startDate = startCalendar.toInstant();

        // Prepare next execution
        final Instant actualDate = getNextExecutionTime(trigger, startDate);
        final Calendar actualCalendar = Calendar.getInstance();
        actualCalendar.setTimeInMillis(actualDate.toEpochMilli());

        // Check that the next execution happened NEXT day
        Assert.assertEquals(startCalendar.get(Calendar.DATE) + 1, actualCalendar.get(Calendar.DATE));
        // At 12h00
        Assert.assertEquals(TWELVE, actualCalendar.get(Calendar.HOUR_OF_DAY));
        Assert.assertEquals(0, actualCalendar.get(Calendar.MINUTE));
        Assert.assertEquals(0, actualCalendar.get(Calendar.SECOND));
    }

    /**
     * Check that the system triggers the weekly cron at the right moments.
     */
    @Test
    @Requirement("REGARDS_DSL_STO_CMD_140")
    @Purpose("Check that the system triggers the weekly cron at the right moments.")
    public void testWeeklyCron() {
        final CronTrigger trigger = new CronTrigger(weeklyCron);

        // Prepare a randomly chosen start instant (the 26th is a Wednesday)
        final Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(TWO_THOUSAND_AND_SIXTEEN, Calendar.OCTOBER, TWENTY_SIX, TWELVE, 0, 0);
        final Instant startDate = startCalendar.toInstant();

        // Prepare next execution
        final Instant actualDate = getNextExecutionTime(trigger, startDate);
        final Calendar actualCalendar = Calendar.getInstance();
        actualCalendar.setTimeInMillis(actualDate.toEpochMilli());

        // Check the next execution happened the next week
        Assert.assertEquals(actualCalendar.get(Calendar.WEEK_OF_YEAR), startCalendar.get(Calendar.WEEK_OF_YEAR) + 1);
        // On monday
        Assert.assertEquals(actualCalendar.get(Calendar.DAY_OF_WEEK), Calendar.MONDAY);
        // At 12h00
        Assert.assertEquals(actualCalendar.get(Calendar.HOUR_OF_DAY), TWELVE);
        Assert.assertEquals(actualCalendar.get(Calendar.MINUTE), 0);
        Assert.assertEquals(actualCalendar.get(Calendar.SECOND), 0);
    }

    /**
     * Check that the system triggers the monthly cron at the right moments.
     */
    @Test
    @Requirement("REGARDS_DSL_STO_CMD_140")
    @Purpose("Check that the system triggers the monthly cron at the right moments.")
    public void testMonthlyCron() {
        final CronTrigger trigger = new CronTrigger(monthlyCron);

        // Prepare a randomly chosen start instant
        final Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(TWO_THOUSAND_AND_SIXTEEN, Calendar.OCTOBER, TWENTY_SIX, TWELVE, 0, 0);
        final Instant startDate = startCalendar.toInstant();

        // Prepare next execution
        final Instant actualDate = getNextExecutionTime(trigger, startDate);
        final Calendar actualCalendar = Calendar.getInstance();
        actualCalendar.setTimeInMillis(actualDate.toEpochMilli());

        // Check the next execution happened the next month
        Assert.assertEquals(actualCalendar.get(Calendar.MONTH), startCalendar.get(Calendar.MONTH) + 1);
        // On the first monday of the month
        Assert.assertEquals(actualCalendar.get(Calendar.DAY_OF_WEEK), Calendar.MONDAY);
        Assert.assertTrue(actualCalendar.get(Calendar.DAY_OF_MONTH) <= SEVEN);
        // At 12h00
        Assert.assertEquals(actualCalendar.get(Calendar.HOUR_OF_DAY), TWELVE);
        Assert.assertEquals(actualCalendar.get(Calendar.MINUTE), 0);
        Assert.assertEquals(actualCalendar.get(Calendar.SECOND), 0);
    }

    /**
     * Get next execution time of passed {@link CronTrigger} with passed start time
     *
     * @param cronTrigger           The cron trigger
     * @param previousExecutionTime The start date
     * @return The next execution time
     */
    private Instant getNextExecutionTime(final CronTrigger cronTrigger, final Instant previousExecutionTime) {
        return cronTrigger.nextExecution(new TriggerContext() {

            @Override
            public Instant lastScheduledExecution() {
                return previousExecutionTime;
            }

            @Override
            public Instant lastActualExecution() {
                return previousExecutionTime;
            }

            @Override
            public Instant lastCompletion() {
                return previousExecutionTime;
            }
        });
    }

}
