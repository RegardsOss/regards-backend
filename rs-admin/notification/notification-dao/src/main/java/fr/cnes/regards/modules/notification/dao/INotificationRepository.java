/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.notification.dao;

import java.util.List;

import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;

/**
 * Interface for an JPA auto-generated CRUD repository managing Notifications.<br>
 * Embeds paging/sorting abilities by entending {@link PagingAndSortingRepository}.<br>
 * Allows execution of Query by Example {@link Example} instances.
 *
 * @author CS SI
 */
public interface INotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all notifications having the passed project user or the passed role as recipient.
     *
     * @param pProjectUser
     *            The required project user recipient
     * @param pRole
     *            The required role recipient
     * @return The list of found notifications
     */
    @Query("select distinct n from T_NOTIFICATION n where ?1 in n.projectUserRecipients or ?2 in n.roleRecipients")
    List<Notification> findByRecipientsContaining(ProjectUser pProjectUser, Role pRole);

    /**
     * Find all notifications with passed <code>status</code>
     *
     * @param pStatus
     *            The notification status
     * @return The list of notifications
     */
    List<Notification> findByStatus(NotificationStatus pStatus);

}
