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
package fr.cnes.regards.modules.notification.domain.dto;

import java.util.List;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.notification.domain.Notification;

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
     * The recipients as project user's logins
     */
    private List<String> projectUserRecipients;

    /**
     * The recipients as role names
     */
    private List<String> roleRecipients;

    /**
     * The notification sender<br>
     * {@link ProjectUser} <code>login</code> or microservice name as a permissive String
     */
    private String sender;

    /**
     * The title
     */
    private String title;

    public NotificationDTO() {
    }

    public NotificationDTO(String message, List<String> projectUserRecipients, List<String> roleRecipients,
            String sender, String title) {
        this.message = message;
        this.projectUserRecipients = projectUserRecipients;
        this.roleRecipients = roleRecipients;
        this.sender = sender;
        this.title = title;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the projectUserRecipients
     */
    public List<String> getProjectUserRecipients() {
        return projectUserRecipients;
    }

    /**
     * @return the roleRecipients
     */
    public List<String> getRoleRecipients() {
        return roleRecipients;
    }

    /**
     * @return the sender
     */
    public String getSender() {
        return sender;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param pMessage
     *            the message to set
     */
    public void setMessage(final String pMessage) {
        message = pMessage;
    }

    /**
     * @param pProjectUserRecipients
     *            the projectUserRecipients to set
     */
    public void setProjectUserRecipients(final List<String> pProjectUserRecipients) {
        projectUserRecipients = pProjectUserRecipients;
    }

    /**
     * @param pRoleRecipients
     *            the roleRecipients to set
     */
    public void setRoleRecipients(final List<String> pRoleRecipients) {
        roleRecipients = pRoleRecipients;
    }

    /**
     * @param pSender
     *            the sender to set
     */
    public void setSender(final String pSender) {
        sender = pSender;
    }

    /**
     * @param pTitle
     *            the title to set
     */
    public void setTitle(final String pTitle) {
        title = pTitle;
    }

}
