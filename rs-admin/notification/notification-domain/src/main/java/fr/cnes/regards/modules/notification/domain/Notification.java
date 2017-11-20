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
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * Models a notification.<br>
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
@Entity(name = "t_notification")
@SequenceGenerator(name = "notificationSequence", initialValue = 1, sequenceName = "seq_notification")
public class Notification implements IIdentifiable<Long> {

    /**
     * The date of the notification
     */
    @Column(name = "date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime date;

    /**
     * Unique Identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notificationSequence")
    @Column(name = "id")
    private Long id;

    /**
     * The message
     */
    @NotBlank
    @Column(name = "message")
    private String message;

    /**
     * The {@link ProjectUser} recipients
     */
    @NotNull
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "ta_notification_projectuser",
            joinColumns = @JoinColumn(name = "notification_id", referencedColumnName = "id",
                    foreignKey = @javax.persistence.ForeignKey(name = "fk_notification_projectuser")),
            inverseJoinColumns = @JoinColumn(name = "projectuser_id", referencedColumnName = "id",
                    foreignKey = @javax.persistence.ForeignKey(name = "fk_projectuser_notification")))
    private List<ProjectUser> projectUserRecipients;

    /**
     * The {@link Role} recipients
     */
    @NotNull
    @Valid
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "ta_notification_role",
            joinColumns = @JoinColumn(name = "notification_id", referencedColumnName = "id",
                    foreignKey = @javax.persistence.ForeignKey(name = "fk_notification_role")),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id",
                    foreignKey = @javax.persistence.ForeignKey(name = "fk_role_notification")))
    private List<Role> roleRecipients;

    /**
     * The notification sender<br>
     * {@link ProjectUser} <code>login</code> or microservice name as a permissive String
     */
    @NotBlank
    @Column(name = "sender")
    private String sender;

    /**
     * The status read or unread
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private NotificationStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private NotificationType type;

    /**
     * The title
     */
    @Column(name = "title")
    private String title;

    /**
     * @return the date
     */
    public OffsetDateTime getDate() {
        return date;
    }

    /**
     * @param pDate
     *            the date to set
     */
    public void setDate(final OffsetDateTime pDate) {
        date = pDate;
    }

    @Override
    public Long getId() {
        return id;
    }

    /**
     * @param pId
     *            the id to set
     */
    public void setId(final Long pId) {
        id = pId;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param pMessage
     *            the message to set
     */
    public void setMessage(final String pMessage) {
        message = pMessage;
    }

    /**
     * @return the projectUserRecipients
     */
    public List<ProjectUser> getProjectUserRecipients() {
        return projectUserRecipients;
    }

    /**
     * @param pProjectUserRecipients
     *            the projectUserRecipients to set
     */
    public void setProjectUserRecipients(final List<ProjectUser> pProjectUserRecipients) {
        projectUserRecipients = pProjectUserRecipients;
    }

    /**
     * @return the roleRecipients
     */
    public List<Role> getRoleRecipients() {
        return roleRecipients;
    }

    /**
     * @param pRoleRecipients
     *            the roleRecipients to set
     */
    public void setRoleRecipients(final List<Role> pRoleRecipients) {
        roleRecipients = pRoleRecipients;
    }

    /**
     * @return the sender
     */
    public String getSender() {
        return sender;
    }

    /**
     * @param pSender
     *            the sender to set
     */
    public void setSender(final String pSender) {
        sender = pSender;
    }

    /**
     * @return the status
     */
    public NotificationStatus getStatus() {
        return status;
    }

    /**
     * @param pStatus
     *            the status to set
     */
    public void setStatus(final NotificationStatus pStatus) {
        status = pStatus;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param pTitle
     *            the title to set
     */
    public void setTitle(final String pTitle) {
        title = pTitle;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }
}
