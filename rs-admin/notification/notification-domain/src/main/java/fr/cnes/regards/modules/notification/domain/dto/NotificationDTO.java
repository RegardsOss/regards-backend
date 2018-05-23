/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Set;

import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationType;

/**
 * DTO representing a {@link Notification}.
 *
 * @author Xavier-Alexandre Brochard
 */
public class NotificationDTO {

    /**
     * The message
     */
    private String message;

    /**
     * The recipients as project user's emails
     */
    private Set<String> projectUserRecipients;

    /**
     * The recipients as role names
     */
    private Set<String> roleRecipients;

    /**
     * The notification sender<br>
     * project user <code>email</code> or microservice name as a permissive String
     */
    private String sender;

    /**
     * The title
     */
    private String title;

    /**
     * The notification type
     */
    private NotificationType type;

    /**
     * Default constructor
     */
    public NotificationDTO() {
    }

    /**
     * Constructor setting the parameters as attributes
     * @param message notification message
     * @param projectUserRecipients users recieving the notification
     * @param roleRecipients roles recieving the notification
     * @param sender notification sender
     * @param title notification title
     * @param type notification type
     */
    public NotificationDTO(String message, Set<String> projectUserRecipients, Set<String> roleRecipients,
            String sender, String title, NotificationType type) {
        this.message = message;
        this.projectUserRecipients = projectUserRecipients;
        this.roleRecipients = roleRecipients;
        this.sender = sender;
        this.title = title;
        this.type = type;
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
    public Set<String> getProjectUserRecipients() {
        return projectUserRecipients;
    }

    /**
     * @param pProjectUserRecipients
     *            the projectUserRecipients to set
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
     * @param pRoleRecipients
     *            the roleRecipients to set
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
     * @param pSender
     *            the sender to set
     */
    public void setSender(final String pSender) {
        sender = pSender;
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

    /**
     * @return the notification type
     */
    public NotificationType getType() {
        return type;
    }

    /**
     * Set the notification type
     * @param type
     */
    public void setType(NotificationType type) {
        this.type = type;
    }
}
