/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.domain.dto;

import fr.cnes.regards.modules.notification.domain.NotificationFrequency;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;

/**
 * DTO representing a {@link NotificationSettings} sent by a client.<br>
 * This way we only expose attributes which we allow to update.
 *
 * @author CS SI
 */
public class NotificationSettingsDTO {

    /**
     * The days frequency of notification
     */
    private Integer days;

    /**
     * The frequency of the notification
     */
    private NotificationFrequency frequency;

    /**
     * The hours frequency of notification<br>
     */
    private Integer hours;

    /**
     * @return the days
     */
    public Integer getDays() {
        return days;
    }

    /**
     * @param pDays
     *            the days to set
     */
    public void setDays(final Integer pDays) {
        days = pDays;
    }

    /**
     * @return the frequency
     */
    public NotificationFrequency getFrequency() {
        return frequency;
    }

    /**
     * @param pFrequency
     *            the frequency to set
     */
    public void setFrequency(final NotificationFrequency pFrequency) {
        frequency = pFrequency;
    }

    /**
     * @return the hours
     */
    public Integer getHours() {
        return hours;
    }

    /**
     * @param pHours
     *            the hours to set
     */
    public void setHours(final Integer pHours) {
        hours = pHours;
    }

}
