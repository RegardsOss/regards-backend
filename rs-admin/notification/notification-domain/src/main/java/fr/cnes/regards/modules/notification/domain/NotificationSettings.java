/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Wraps the different project projectUser's settings available for notifications configuration.
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
     * The settings are specific to a {@link ProjectUser}
     */
    @NotNull
    @OneToOne
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_notification_settings_user"))
    private ProjectUser projectUser;

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
     * @return the projectUser
     */
    public ProjectUser getProjectUser() {
        return projectUser;
    }

    /**
     * @param pDays
     *            the days to set
     */
    public void setDays(final Integer pDays) {
        days = pDays;
    }

    /**
     * @param pFrequency
     *            the frequency to set
     */
    public void setFrequency(final NotificationFrequency pFrequency) {
        frequency = pFrequency;
    }

    /**
     * @param pHours
     *            the hours to set
     */
    public void setHours(final Integer pHours) {
        hours = pHours;
    }

    /**
     * @param pId
     *            the id to set
     */
    public void setId(final Long pId) {
        id = pId;
    }

    /**
     * @param pUser
     *            the projectUser to set
     */
    public void setProjectUser(final ProjectUser pUser) {
        projectUser = pUser;
    }
}
