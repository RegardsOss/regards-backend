/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converter.MimeTypeConverter;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.notification.NotificationLevel;

/**
 * Models a notification.<br>
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
@Entity
@Table(name = "t_notification")
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
     * The role recipients represented by their name
     */
    @NotNull
    @ElementCollection
    @CollectionTable(name = "ta_notification_role_name", joinColumns = @JoinColumn(name = "notification_id"),
            foreignKey = @javax.persistence.ForeignKey(name = "fk_notification_role_name_notification_id"))
    @Column(name = "role_name", length = 200)
    private Set<String> roleRecipients;

    /**
     * The project user recipients represented by their email
     */
    @NotNull
    @ElementCollection
    @CollectionTable(name = "ta_notification_projectuser_email", joinColumns = @JoinColumn(name = "notification_id"),
            foreignKey = @javax.persistence.ForeignKey(name = "fk_notification_projectuser_email_notification_id"))
    @Column(name = "projectuser_email", length = 200)
    private Set<String> projectUserRecipients;

    /**
     * The notification sender<br>
     * project user <code>email</code> or microservice name as a permissive String
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

    /**
     * The notification type
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private NotificationLevel level;

    /**
     * The title
     */
    @Column(name = "title")
    private String title;

    @Convert(converter = MimeTypeConverter.class)
    @Column(name = "mime_type", length = 255)
    private MimeType mimeType = MimeTypeUtils.TEXT_PLAIN;

    /**
     * @return the date
     */
    public OffsetDateTime getDate() {
        return date;
    }

    /**
     * @param pDate the date to set
     */
    public void setDate(final OffsetDateTime pDate) {
        date = pDate;
    }

    @Override
    public Long getId() {
        return id;
    }

    /**
     * @param pId the id to set
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
     * @param pMessage the message to set
     */
    public void setMessage(final String pMessage) {
        message = pMessage;
    }

    /**
     * @return the projectUserRecipients
     */
    public Set<String> getProjectUserRecipients() {
        return projectUserRecipients;
    }

    /**
     * @param pProjectUserRecipients the projectUserRecipients to set
     */
    public void setProjectUserRecipients(final Set<String> pProjectUserRecipients) {
        projectUserRecipients = pProjectUserRecipients;
    }

    /**
     * @return the roleRecipients
     */
    public Set<String> getRoleRecipients() {
        return roleRecipients;
    }

    /**
     * @param pRoleRecipients the roleRecipients to set
     */
    public void setRoleRecipients(final Set<String> pRoleRecipients) {
        roleRecipients = pRoleRecipients;
    }

    /**
     * @return the sender
     */
    public String getSender() {
        return sender;
    }

    /**
     * @param pSender the sender to set
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
     * @param pStatus the status to set
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
     * @param pTitle the title to set
     */
    public void setTitle(final String pTitle) {
        title = pTitle;
    }

    /**
     * @return the notification type
     */
    public NotificationLevel getLevel() {
        return level;
    }

    /**
     * Set the notification type
     */
    public void setLevel(NotificationLevel level) {
        this.level = level;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    public void setMimeType(MimeType mimeType) {
        this.mimeType = mimeType;
    }
}
