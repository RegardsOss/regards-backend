/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * Test class for {@link CronTest}.
 *
 * @author xbrochar
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = { CronTestConfiguration.class })
public class CronTest {

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
     * Check that the system triggers the daily cron at the right moments.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system triggers the daily cron at the right moments.")
    public void testDailyCron() {
        final CronTrigger trigger = new CronTrigger(dailyCron);
        final Calendar today = Calendar.getInstance();
        today.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);

        final SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss EEEE");
        final Date yesterday = today.getTime();
        final Date nextExecutionTime = trigger.nextExecutionTime(new TriggerContext() {

            @Override
            public Date lastScheduledExecutionTime() {
                return yesterday;
            }

            @Override
            public Date lastActualExecutionTime() {
                return yesterday;
            }

            @Override
            public Date lastCompletionTime() {
                return yesterday;
            }
        });

        final String message = "Next Execution date: " + df.format(nextExecutionTime);
        System.out.println(message);
    }

    /**
     * Check that the system triggers the weekly cron at the right moments.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system triggers the weekly cron at the right moments.")
    public void testWeeklyCron() {
        final CronTrigger trigger = new CronTrigger(weeklyCron);
        Assert.fail("TODO");
    }

    /**
     * Check that the system triggers the monthly cron at the right moments.
     */
    @Test
    @Requirement("?")
    @Purpose("Check that the system triggers the monthly cron at the right moments.")
    public void testMonthlyCron() {
        final CronTrigger trigger = new CronTrigger(monthlyCron);
        Assert.fail("TODO");
    }

}
