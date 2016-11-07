/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.domain;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * Models a notification.<br>
 *
 * @author CS SI
 */
@Entity(name = "T_NOTIFICATION")
@SequenceGenerator(name = "notificationSequence", initialValue = 1, sequenceName = "SEQ_NOTIFICATION")
public class Notification implements IIdentifiable<Long> {

    /**
     * The date of the notification
     */
    @Column(name = "date")
    private LocalDateTime date;

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
    @OneToMany
    @JoinColumn(name = "notification_id", foreignKey = @ForeignKey(name = "FK_NOTIFICATION_PROJECT_USER_RECIPIENTS"))
    private List<ProjectUser> projectUserRecipients;

    /**
     * The {@link Role} recipients
     */
    @NotNull
    @Valid
    @OneToMany
    @JoinColumn(name = "notification_id", foreignKey = @ForeignKey(name = "FK_NOTIFICATION_ROLE_RECIPIENTS"))
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
    @Column(name = "status")
    private NotificationStatus status;

    /**
     * The title
     */
    @Column(name = "title")
    private String title;

    /**
     * @return the date
     */
    public LocalDateTime getDate() {
        return date;
    }

    @Override
    public Long getId() {
        return id;
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
    public List<ProjectUser> getProjectUserRecipients() {
        return projectUserRecipients;
    }

    /**
     * @return the roleRecipients
     */
    public List<Role> getRoleRecipients() {
        return roleRecipients;
    }

    /**
     * @return the sender
     */
    public String getSender() {
        return sender;
    }

    /**
     * @return the status
     */
    public NotificationStatus getStatus() {
        return status;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param pDate
     *            the date to set
     */
    public void setDate(final LocalDateTime pDate) {
        date = pDate;
    }

    /**
     * @param pId
     *            the id to set
     */
    public void setId(final Long pId) {
        id = pId;
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
    public void setProjectUserRecipients(final List<ProjectUser> pProjectUserRecipients) {
        projectUserRecipients = pProjectUserRecipients;
    }

    /**
     * @param pRoleRecipients
     *            the roleRecipients to set
     */
    public void setRoleRecipients(final List<Role> pRoleRecipients) {
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
     * @param pStatus
     *            the status to set
     */
    public void setStatus(final NotificationStatus pStatus) {
        status = pStatus;
    }

    /**
     * @param pTitle
     *            the title to set
     */
    public void setTitle(final String pTitle) {
        title = pTitle;
    }

}
