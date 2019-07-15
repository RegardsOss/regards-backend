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
package fr.cnes.regards.modules.notification.dao;

import java.util.List;
import java.util.Optional;

import fr.cnes.regards.modules.notification.domain.INotificationWithoutMessage;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import org.springframework.data.repository.query.Param;

/**
 * Interface for an JPA auto-generated CRUD repository managing Notifications.<br>
 * Embeds paging/sorting abilities by entending {@link PagingAndSortingRepository}.<br>
 * Allows execution of Query by Example {@link Example} instances.
 * @author Xavier-Alexandre Brochard
 */
public interface INotificationRepository
        extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    /**
     * Find all notifications having given project user or given role as recipient.
     * @param projectUser The required project user recipient
     * @param role The required role recipient
     * @return The list of found notifications
     */
    default Page<INotificationWithoutMessage> findByRecipientsContaining(String projectUser, String role, Pageable pageable) {

        return findByProjectUserRecipientsContainingOrRoleRecipientsContaining(projectUser, role, pageable);
    }

    @EntityGraph(attributePaths = { "projectUserRecipients", "roleRecipients" })
    Page<INotificationWithoutMessage> findByProjectUserRecipientsContainingOrRoleRecipientsContaining(String projectUser, String role,
            Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "projectUserRecipients", "roleRecipients" })
    Optional<Notification> findById(Long id);


    default Page<INotificationWithoutMessage> findAllNotificationsWithoutMessage(Pageable pageable) {
        Page<Long> pageNotifications = findAllId(pageable);

        List<INotificationWithoutMessage> notifs = findAllByIdInOrderByIdDesc(pageNotifications.getContent());

        return new PageImpl<>(notifs, pageable, pageNotifications.getTotalElements());
    }

    @Query(value = "select distinct n.id from Notification n"
            + " ORDER BY id DESC")
    Page<Long> findAllId(Pageable page);

    /**
     * Find all notifications having the passed project user or the passed role as recipient.
     * @param projectUser The required project user recipient
     * @param role The required role recipient
     * @return The list of found notifications
     */
    default Page<INotificationWithoutMessage> findByStatusAndRecipientsContaining(NotificationStatus status, String projectUser,
                                                                                  String role, Pageable pageable) {
        // handling pagination by hand here is a bit touchy as we have conditions on joined tables
        // first lets get all notification ids that respect our wishes
        List<Long> allNotifIds = findAllIdByStatusAndRecipientsContainingSortedByIdDesc(status, projectUser, role);
        // now, lets extract ids corresponding to the page wished
        int from = pageable.getPageNumber() * pageable.getPageSize();
        int to = (pageable.getPageNumber() + 1) * pageable.getPageSize();
        int nbNotifs = allNotifIds.size();
        List<Long> pageIds;
        if (to < nbNotifs) {
            pageIds = allNotifIds.subList(from, to);
        } else {
            pageIds = allNotifIds.subList(from, nbNotifs);
        }
        // now let get all the notif according to extracted ids
        List<INotificationWithoutMessage> notifs = findAllByIdInOrderByIdDesc(pageIds);
        // eventually, reconstruct a page
        return new PageImpl<>(notifs, pageable, nbNotifs);
    }

    @EntityGraph(attributePaths = { "projectUserRecipients", "roleRecipients" })
    List<INotificationWithoutMessage> findAllByIdInOrderByIdDesc(List<Long> pageIds);

    @Query(value = "select distinct n.id from Notification n"
            + " where n.status= ?1 and (?2 member of n.projectUserRecipients or "
            + " ?3 member of n.roleRecipients) ORDER BY id DESC")
    List<Long> findAllIdByStatusAndRecipientsContainingSortedByIdDesc(NotificationStatus status, String projectUser,
            String role);

    @Query(value = "select COUNT(distinct n.id) from Notification n"
            + " where n.status= ?1 and (?2 member of n.projectUserRecipients or "
            + " ?3 member of n.roleRecipients) GROUP BY id ORDER BY id DESC")
    Long countByStatus(NotificationStatus status, String projectUser, String role);

    /**
     * Find all notifications with passed <code>status</code>
     * @param pStatus The notification status
     * @return The list of notifications
     */
    @EntityGraph(attributePaths = { "projectUserRecipients", "roleRecipients" })
    Page<Notification> findByStatus(NotificationStatus pStatus, Pageable page);

    /**
     * Find all notifications without message with passed <code>status</code>
     * @param status The notification status
     * @return The list of notifications
     */
    default Page<INotificationWithoutMessage> findAllNotificationsWithoutMessageByStatus(NotificationStatus status, Pageable pageable) {
        Page<Long> pageNotifications = findPageIdByStatus(status, pageable);

        List<INotificationWithoutMessage> notifs = findAllByIdInOrderByIdDesc(pageNotifications.getContent());

        return new PageImpl<>(notifs, pageable, pageNotifications.getTotalElements());
    }

    @Query(value = "select distinct n.id from Notification n"
            + " where n.status = :status"
            + " order by id desc")
    Page<Long> findPageIdByStatus(@Param("status") NotificationStatus status, Pageable pageable);

    Long countByStatus(NotificationStatus pStatus);

    /**
     * Find all notifications which recipients contains the given user, represented by its email
     * @return all notifications which recipients contains the given user, represented by its email
     */
    Page<Notification> findAllByProjectUserRecipientsContaining(String email, Pageable page);

    /**
     * Find all notifications which recipients contains the given role, represented by its name
     * @return all notifications which recipients contains the given role, represented by its name
     */
    Page<Notification> findAllByRoleRecipientsContaining(String role, Pageable page);

    @Modifying
    @Query(value = "UPDATE {h-schema}t_notification set status = ?1 FROM {h-schema}ta_notification_role_name recipient "
            + "WHERE t_notification.id = recipient.notification_id AND recipient.role_name = ?2", nativeQuery = true)
    void updateAllNotificationStatusByRole(String status, String role);

    @Modifying
    @Query(value = "UPDATE {h-schema}t_notification set status = ?1 FROM {h-schema}ta_notification_projectuser_email "
            + "recipient WHERE t_notification.id = recipient.notification_id AND recipient.projectuser_email = ?2",
            nativeQuery = true)
    void updateAllNotificationStatusByUser(String status, String projectUser);
}
