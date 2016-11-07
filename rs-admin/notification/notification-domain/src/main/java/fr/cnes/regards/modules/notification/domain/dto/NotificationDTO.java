/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.domain.dto;

import java.util.List;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.notification.domain.Notification;

/**
 * DTO representing a {@link Notification}.
 *
 * @author CS SI
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
