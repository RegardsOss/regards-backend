/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
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
@Repository
public interface INotificationRepository
    extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    Logger LOGGER = LoggerFactory.getLogger(INotificationRepository.class);

    @Override
    @EntityGraph(attributePaths = { "projectUserRecipients", "roleRecipients" },
                 type = EntityGraph.EntityGraphType.LOAD)
    Optional<Notification> findById(Long id);

    @Query(value = "select count(notif.id) from {h-schema}t_notification notif "
                   + "left join {h-schema}ta_notification_projectuser_email pu on notif.id=pu.notification_id "
                   + "left join {h-schema}ta_notification_role_name role on notif.id=role.notification_id "
                   + "where notif.status=:status and (pu.projectuser_email=:user or role.role_name=:role)",
           nativeQuery = true)
        //This is a native query so we need to pass status as string and not as Enum
    Long countByStatus(@Param("status") String status, @Param("user") String projectUser, @Param("role") String role);

    /**
     * Find all notifications with passed <code>status</code>
     *
     * @param status The notification status
     * @return The list of notifications
     */
    default Page<Notification> findByStatus(NotificationStatus status, Pageable pageable) {
        Page<Long> idPage = findIdPageByStatus(status, pageable);
        List<Notification> notifs = findAllNotificationByIdInOrderByIdDesc(idPage.stream()
                                                                                 .collect(Collectors.toList()));
        return new PageImpl<>(notifs, idPage.getPageable(), idPage.getTotalElements());
    }

    @EntityGraph(attributePaths = { "projectUserRecipients", "roleRecipients" },
                 type = EntityGraph.EntityGraphType.LOAD)
    List<Notification> findAllNotificationByIdInOrderByIdDesc(List<Long> ids);

    @Query(value = "select id from Notification where status=:status", countQuery = "select count(1) from Notification where status=:status")
    Page<Long> findIdPageByStatus(@Param("status") NotificationStatus status, Pageable pageable);

    Long countByStatus(NotificationStatus pStatus);

    @Modifying
    @Query(value = "UPDATE {h-schema}t_notification set status = ?1 FROM {h-schema}ta_notification_role_name recipient "
                   + "WHERE t_notification.id = recipient.notification_id AND recipient.role_name = ?2",
           nativeQuery = true)
    void updateAllNotificationStatusByRole(String status, String role);

    @Modifying
    @Query(value = "UPDATE {h-schema}t_notification set status = ?1 FROM {h-schema}ta_notification_projectuser_email "
                   + "recipient WHERE t_notification.id = recipient.notification_id AND recipient.projectuser_email = ?2",
           nativeQuery = true)
    void updateAllNotificationStatusByUser(String status, String projectUser);

}
