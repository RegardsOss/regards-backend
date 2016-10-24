/*
 * LICENSE_PLACEHOLDER
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

import org.springframework.hateoas.Identifiable;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Wraps the different project user's settings available for notifications configuration.
 *
 * @author CS SI
 */
@Entity(name = "T_NOTIFICATION_SETTINGS")
@SequenceGenerator(name = "notificationSettingsSequence", initialValue = 1, sequenceName = "SEQ_NOTIFICATION_SETTINGS")
public class NotificationSettings implements Identifiable<Long> {

    /**
     * Self expl
     */
    private static final long HOURS_IN_A_DAY = 24L;

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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notificationSequence")
    @Column(name = "id")
    private Long id;

    /**
     * The settings are specific to a {@link ProjectUser}
     */
    @NotNull
    @OneToOne
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "FK_NOTIFICATION_SETTINGS_USER"))
    private ProjectUser user;

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
     * @return the user
     */
    public ProjectUser getUser() {
        return user;
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
     *            the user to set
     */
    public void setUser(final ProjectUser pUser) {
        user = pUser;
    }
}
