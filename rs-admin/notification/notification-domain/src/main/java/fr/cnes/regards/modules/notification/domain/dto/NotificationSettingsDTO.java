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
package fr.cnes.regards.modules.notification.domain.dto;

import fr.cnes.regards.modules.notification.domain.NotificationFrequency;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;

/**
 * DTO representing a {@link NotificationSettings} sent by a client.<br>
 * This way we only expose attributes which we allow to update.
 *
 * @author Xavier-Alexandre Brochard
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
     * @param pDays the days to set
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
     * @param pFrequency the frequency to set
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
     * @param pHours the hours to set
     */
    public void setHours(final Integer pHours) {
        hours = pHours;
    }

}
