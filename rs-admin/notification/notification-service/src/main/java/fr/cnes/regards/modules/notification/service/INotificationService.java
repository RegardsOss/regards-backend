/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.service;

import java.util.List;
import java.util.stream.Stream;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import fr.cnes.regards.modules.notification.domain.dto.NotificationDTO;

/**
 * Strategy interface to handle CRUD operations on Notification entities
 *
 * @author CS SI
 */
public interface INotificationService {

    /**
     * Retrieve the list of notifications intended for the logged user, trough the project user or their role.
     *
     * @return A {@link List} of {@link Notification}
     */
    List<Notification> retrieveNotifications();

    /**
     * Save a new notification in db for later sending by a scheluder.
     *
     * @param pDto
     *            A DTO for easy parsing of the response body. Mapping to true {@link Notification} is expected to be
     *            done here.
     * @return The sent {@link Notification}
     */
    Notification createNotification(NotificationDTO pDto);

    /**
     * Retrieve a notification
     *
     * @param pId
     *            The notification <code>id</code>
     * @return The {@link Notification}
     * @throws EntityNotFoundException
     *             Thrown when no notification with passed <code>id</code> could be found
     */
    Notification retrieveNotification(Long pId) throws EntityNotFoundException;

    /**
     * Update the {@link Notification#status}
     *
     * @param pId
     *            The notification <code>id</code>
     * @param pStatus
     *            The new status value
     * @throws EntityNotFoundException
     *             Thrown when no notification with passed <code>id</code> could be found
     */
    void updateNotificationStatus(Long pId, NotificationStatus pStatus) throws EntityNotFoundException;

    /**
     * Delete a notification
     *
     * @param pId
     *            The notification <code>id</code>
     * @throws EntityNotFoundException
     *             Thrown when no notification with passed <code>id</code> could be found
     */
    void deleteNotification(Long pId) throws EntityNotFoundException;

    /**
     * Retrieve all notifications which should be sent
     *
     * @return The list of notifications
     */
    List<Notification> retrieveNotificationsToSend();

    /**
     * Gather the list of recipients on a notification
     *
     * @param pNotification
     *            The notification
     * @return The stream of project users
     */
    Stream<ProjectUser> findRecipients(Notification pNotification);

}
