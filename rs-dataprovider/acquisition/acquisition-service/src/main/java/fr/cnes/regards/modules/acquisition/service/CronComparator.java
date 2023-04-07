package fr.cnes.regards.modules.acquisition.service;

import org.springframework.scheduling.support.CronExpression;

import java.time.OffsetDateTime;

/**
 * This class allows to check if the next date of the provided cron expression will occurs in the next minute
 */
public final class CronComparator {

    private CronComparator() {
    }

    /**
     * Compare the provided cron expression and the provided last check date and the current date.
     * If the next date of the cron expression after the provided last check date is before the provided current date,
     * the cron should be run.
     *
     * @param cronExpression the cron expression (with seconds)
     * @param lastCheckDate  the current date
     * @return true if the cron would occur before the provided currentDate
     */
    public static boolean shouldRun(String cronExpression, OffsetDateTime lastCheckDate, OffsetDateTime currentDate) {
        CronExpression cron = CronExpression.parse(cronExpression);
        OffsetDateTime nextDate = cron.next(lastCheckDate);
        return nextDate != null && (nextDate.isEqual(currentDate) || nextDate.isBefore(currentDate));
    }
}
