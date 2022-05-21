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
package fr.cnes.regards.modules.notification.dao;

import fr.cnes.regards.modules.notification.domain.INotificationWithoutMessage;
import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Interface for an JPA auto-generated CRUD repository managing Notifications.<br>
 * Embeds paging/sorting abilities by extending {@link PagingAndSortingRepository}.<br>
 * Allows execution of Query by Example {@link Example} instances.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface INotificationRepository
    extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    Logger LOGGER = LoggerFactory.getLogger(INotificationRepository.class);

    /**
     * Find all notifications having given project user or given role as recipient.
     *
     * @param projectUser The required project user recipient
     * @param role        The required role recipient
     * @return The list of found notifications
     */
    default Page<INotificationWithoutMessage> findByRecipientsContaining(String projectUser,
                                                                         String role,
                                                                         Pageable pageable) {

        Page<BigInteger> idPage = findIdPageByRecipientsContaining(projectUser, role, pageable);
        List<INotificationWithoutMessage> notifs = findAllByIdInOrderByIdDesc(idPage.getContent()
                                                                                    .stream()
                                                                                    .map(BigInteger::longValue)
                                                                                    .collect(Collectors.toList()));
        return new PageImpl<>(notifs, idPage.getPageable(), idPage.getTotalElements());
    }

    @Query(value = "select notif.id from {h-schema}t_notification notif "
        + "left join {h-schema}ta_notification_projectuser_email pu on notif.id=pu.notification_id "
        + "left join {h-schema}ta_notification_role_name role on notif.id=role.notification_id "
        + "where pu.projectuser_email=:user or role.role_name=:role",
        countQuery = "select count(notif.id) from {h-schema}t_notification notif "
            + "left join {h-schema}ta_notification_projectuser_email pu on notif.id=pu.notification_id "
            + "left join {h-schema}ta_notification_role_name role on notif.id=role.notification_id "
            + "where pu.projectuser_email=:user or role.role_name=:role", nativeQuery = true)
    Page<BigInteger> findIdPageByRecipientsContaining(@Param("user") String projectUser,
                                                      @Param("role") String role,
                                                      Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "projectUserRecipients", "roleRecipients" },
        type = EntityGraph.EntityGraphType.LOAD)
    Optional<Notification> findById(Long id);

    default Page<INotificationWithoutMessage> findAllNotificationsWithoutMessage(Pageable pageable) {
        Page<Long> pageNotifications = findAllId(pageable);

        List<INotificationWithoutMessage> notifs = findAllByIdInOrderByIdDesc(pageNotifications.getContent());

        return new PageImpl<>(notifs, pageable, pageNotifications.getTotalElements());
    }

    @Query(value = "select n.id from Notification n ORDER BY id DESC")
    Page<Long> findAllId(Pageable page);

    /**
     * Find all notifications having the passed project user or the passed role as recipient.
     *
     * @param projectUser The required project user recipient
     * @param role        The required role recipient
     * @return The list of found notifications
     */
    default Page<INotificationWithoutMessage> findByStatusAndRecipientsContaining(NotificationStatus status,
                                                                                  String projectUser,
                                                                                  String role,
                                                                                  Pageable pageable) {
        LOGGER.trace("----------------------------- STARTING findByStatusAndRecipientsContaining");
        // handling pagination by hand here is a bit touchy as we have conditions on joined tables
        // first lets get all notification ids that respect our wishes
        LOGGER.trace("----------------------------- STARTING find id page");
        Page<BigInteger> pageIds = findAllIdByStatusAndRecipientsContainingSortedByIdDesc(status.toString(),
                                                                                          projectUser,
                                                                                          role,
                                                                                          pageable);
        LOGGER.trace("----------------------------- ENDING find id page");
        // now let get all the notif according to extracted ids
        LOGGER.trace("----------------------------- STARTING notif without message by ids");
        List<INotificationWithoutMessage> notifs = findAllByIdInOrderByIdDesc(pageIds.stream()
                                                                                     .map(BigInteger::longValue)
                                                                                     .collect(Collectors.toList()));
        LOGGER.trace("----------------------------- ENDING findByStatusAndRecipientsContaining");
        // eventually, reconstruct a page
        return new PageImpl<>(notifs, pageable, pageIds.getTotalElements());
    }

    @Query(value = "SELECT n.id as id, n.date as date, n.sender as sender, n.status as status, n.level as level, "
        + "n.title as title, n.mimeType as mimeType FROM Notification n WHERE n.id in :ids")
    List<INotificationWithoutMessage> findAllByIdInOrderByIdDesc(@Param("ids") List<Long> ids);

    @Query(value = "select notif.id from {h-schema}t_notification notif "
        + "left join {h-schema}ta_notification_projectuser_email pu on notif.id=pu.notification_id "
        + "left join {h-schema}ta_notification_role_name role on notif.id=role.notification_id "
        + "where notif.status=:status and (pu.projectuser_email=:user or role.role_name=:role)",
        countQuery = "select count(notif.id) from {h-schema}t_notification notif "
            + "left join {h-schema}ta_notification_projectuser_email pu on notif.id=pu.notification_id "
            + "left join {h-schema}ta_notification_role_name role on notif.id=role.notification_id "
            + "where notif.status=:status and (pu.projectuser_email=:user or role.role_name=:role)", nativeQuery = true)
        //This is a native query so we need to pass status as string and not as Enum
    Page<BigInteger> findAllIdByStatusAndRecipientsContainingSortedByIdDesc(@Param("status") String status,
                                                                            @Param("user") String projectUser,
                                                                            @Param("role") String role,
                                                                            Pageable pageable);

    @Query(value = "select count(notif.id) from {h-schema}t_notification notif "
        + "left join {h-schema}ta_notification_projectuser_email pu on notif.id=pu.notification_id "
        + "left join {h-schema}ta_notification_role_name role on notif.id=role.notification_id "
        + "where notif.status=:status and (pu.projectuser_email=:user or role.role_name=:role)", nativeQuery = true)
        //This is a native query so we need to pass status as string and not as Enum
    Long countByStatus(@Param("status") String status, @Param("user") String projectUser, @Param("role") String role);

    @Modifying
    @Query("delete from Notification n where (:role member of n.roleRecipients)  AND n.status = :status")
    void deleteByStatusAndRoleRecipientsInAndProjectUserRecipientsIsNull(@Param("status") NotificationStatus status,
                                                                         @Param("role") String role);

    @Modifying
    @Query("delete from Notification n where (:user member of n.projectUserRecipients)  AND n.status = :status")
    void deleteByStatusAndProjectUserRecipientsIn(@Param("status") NotificationStatus status,
                                                  @Param("user") String user);

    /**
     * Find all notifications with passed <code>status</code>
     *
     * @param status The notification status
     * @return The list of notifications
     */
    default Page<Notification> findByStatus(NotificationStatus status, Pageable pageable) {
        Page<BigInteger> idPage = findIdPageByStatus(status.toString(), pageable);
        List<Notification> notifs = findAllNotifByIdInOrderByIdDesc(idPage.stream()
                                                                          .map(BigInteger::longValue)
                                                                          .collect(Collectors.toList()));
        return new PageImpl<>(notifs, idPage.getPageable(), idPage.getTotalElements());
    }

    @EntityGraph(attributePaths = { "projectUserRecipients", "roleRecipients" },
        type = EntityGraph.EntityGraphType.LOAD)
    List<Notification> findAllNotifByIdInOrderByIdDesc(List<Long> ids);

    @Query(value = "select notif.id from {h-schema}t_notification notif "
        + "left join {h-schema}ta_notification_projectuser_email pu on notif.id=pu.notification_id "
        + "left join {h-schema}ta_notification_role_name role on notif.id=role.notification_id "
        + "where notif.status=:status", countQuery = "select count(notif.id) from {h-schema}t_notification notif "
        + "left join {h-schema}ta_notification_projectuser_email pu on notif.id=pu.notification_id "
        + "left join {h-schema}ta_notification_role_name role on notif.id=role.notification_id "
        + "where notif.status=:status", nativeQuery = true)
    Page<BigInteger> findIdPageByStatus(@Param("status") String status, Pageable pageable);

    /**
     * Find all notifications without message with passed <code>status</code>
     *
     * @param status The notification status
     * @return The list of notifications
     */
    default Page<INotificationWithoutMessage> findAllNotificationsWithoutMessageByStatus(NotificationStatus status,
                                                                                         Pageable pageable) {
        Page<Long> pageNotifications = findPageIdByStatus(status, pageable);

        List<INotificationWithoutMessage> notifs = findAllByIdInOrderByIdDesc(pageNotifications.getContent());

        return new PageImpl<>(notifs, pageable, pageNotifications.getTotalElements());
    }

    @Query(value = "select n.id from Notification n where n.status = :status order by id desc")
    Page<Long> findPageIdByStatus(@Param("status") NotificationStatus status, Pageable pageable);

    Long countByStatus(NotificationStatus pStatus);

    @Modifying
    @Query(value = "UPDATE {h-schema}t_notification set status = ?1 FROM {h-schema}ta_notification_role_name recipient "
        + "WHERE t_notification.id = recipient.notification_id AND recipient.role_name = ?2", nativeQuery = true)
    void updateAllNotificationStatusByRole(String status, String role);

    @Modifying
    @Query(value = "UPDATE {h-schema}t_notification set status = ?1 FROM {h-schema}ta_notification_projectuser_email "
        + "recipient WHERE t_notification.id = recipient.notification_id AND recipient.projectuser_email = ?2",
        nativeQuery = true)
    void updateAllNotificationStatusByUser(String status, String projectUser);

    void deleteByIdIn(Collection<Long> idsToDelete);
}
