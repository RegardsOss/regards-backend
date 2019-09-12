package fr.cnes.regards.modules.acquisition.service;

import java.time.OffsetDateTime;
import java.util.Date;
import org.springframework.scheduling.support.CronSequenceGenerator;

/**
 * This class allows to check if the next date of the provided cron expression will occurs in the next minute
 */
public class CronComparator {

    /**
     * This value is correlated to the ScheduledDataProviderTasks scheduled expression
     * If the next cron value is within 1 min, it will be run
     */
    public static final int CRON_REPETITION = 60000;

    /**
     * Compare the provided cron expression and the current date
     * @param cronExpression the cron expression (with seconds)
     * @return true if the cron would occur in the next minute
     */
    public static boolean shouldRun(String cronExpression) {
        return CronComparator.shouldRun(cronExpression, OffsetDateTime.now().toEpochSecond() * 1000);
    }


    /**
     * Compare the provided cron expression and the provided date
     * @param cronExpression the cron expression (with seconds)
     * @param currentDate the current date
     * @return true if the cron would occur in the next minute
     */
    public static boolean shouldRun(String cronExpression, Long currentDate) {
        CronSequenceGenerator cron = new CronSequenceGenerator(cronExpression);
        Date nextDate = cron.next(new Date(currentDate));
        return CronComparator.shouldRun(currentDate, nextDate.getTime());
    }

    /**
     * @param currentDate current date timestamp in ms
     * @param nextRun next run date timestamp in ms
     * @return true if the cron would occur in the next minute
     */
    public static boolean shouldRun(Long currentDate, Long nextRun) {
        return nextRun - currentDate <= CRON_REPETITION;
    }
}
