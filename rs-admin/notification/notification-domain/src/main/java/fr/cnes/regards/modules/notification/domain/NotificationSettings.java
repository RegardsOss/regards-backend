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
package fr.cnes.regards.modules.notification.domain;

import fr.cnes.regards.framework.jpa.IIdentifiable;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Wraps the different project projectUserEmail's settings available for notifications configuration.
 *
 * @author Xavier-Alexandre Brochard
 */
@Entity(name = "t_notification_settings")
@SequenceGenerator(name = "notificationSettingsSequence", initialValue = 1, sequenceName = "seq_notification_settings")
public class NotificationSettings implements IIdentifiable<Long> {

    /**
     * Self expl
     */
    private static final int HOURS_IN_A_DAY = 24;

    /**
     * The days frequency of notification<br>
     * Only used if <code>frequency</code> is set to {@link NotificationFrequency#CUSTOM}
     */
    @Min(value = 1, message = "The custom notification frequency cannot be inferior to one day.")
    @Column(name = "days")
    private Integer days;

    /**
     * The frequency of the notification
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency")
    private NotificationFrequency frequency;

    /**
     * The hours frequency of notification<br>
     * Only used if <code>frequency</code> is set to {@link NotificationFrequency#CUSTOM}
     */
    @Min(0)
    @Max(HOURS_IN_A_DAY)
    @Column(name = "hours")
    private Integer hours;

    /**
     * The settings unique id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notificationSettingsSequence")
    @Column(name = "id")
    private Long id;

    /**
     * The settings are specific to a project user represented by its email
     */
    @NotNull
    @Column(name = "user_email")
    private String projectUserEmail;

    /**
     * @return the days
     */
    public Integer getDays() {
        return days;
    }

    /**
     * @return the frequency
     */
    public NotificationFrequency getFrequency() {
        return frequency;
    }

    /**
     * @return the hours
     */
    public Integer getHours() {
        return hours;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.hateoas.Identifiable#getId()
     */
    @Override
    public Long getId() {
        return id;
    }

    /**
     * @return the projectUserEmail
     */
    public String getProjectUserEmail() {
        return projectUserEmail;
    }

    /**
     * @param pDays the days to set
     */
    public void setDays(final Integer pDays) {
        days = pDays;
    }

    /**
     * @param pFrequency the frequency to set
     */
    public void setFrequency(final NotificationFrequency pFrequency) {
        frequency = pFrequency;
    }

    /**
     * @param pHours the hours to set
     */
    public void setHours(final Integer pHours) {
        hours = pHours;
    }

    /**
     * @param pId the id to set
     */
    public void setId(final Long pId) {
        id = pId;
    }

    /**
     * @param pUser the projectUserEmail to set
     */
    public void setProjectUserEmail(final String pUser) {
        projectUserEmail = pUser;
    }
}
