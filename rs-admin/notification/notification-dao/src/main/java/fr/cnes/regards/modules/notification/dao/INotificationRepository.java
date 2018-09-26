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

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import fr.cnes.regards.modules.notification.domain.Notification;
import fr.cnes.regards.modules.notification.domain.NotificationStatus;

/**
 * Interface for an JPA auto-generated CRUD repository managing Notifications.<br>
 * Embeds paging/sorting abilities by entending {@link PagingAndSortingRepository}.<br>
 * Allows execution of Query by Example {@link Example} instances.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface INotificationRepository
        extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    /**
     * Find all notifications having the passed project user or the passed role as recipient.
     *
     * @param projectUser The required project user recipient
     * @param role The required role recipient
     * @return The list of found notifications
     */
    @Query(value = "select distinct on (n.id) n.* from {h-schema}t_notification n "
            + "left join {h-schema}ta_notification_role_name r on r.notification_id = n.id "
            + "left join {h-schema}ta_notification_projectuser_email e on e.notification_id = n.id "
            + "where e.projectuser_email = ?1 or r.role_name= ?2 GROUP BY n.id ORDER BY ?#{#pageable}",
            countQuery = "select count(distinct n.id) from {h-schema}t_notification n "
                    + "left join {h-schema}ta_notification_role_name r on r.notification_id = n.id "
                    + "left join {h-schema}ta_notification_projectuser_email e on e.notification_id = n.id "
                    + "where e.projectuser_email = ?1 or r.role_name= ?2 GROUP BY n.id",
            nativeQuery = true)
    Page<Notification> findByRecipientsContaining(String projectUser, String role, Pageable pageable);

    /**
     * Find all notifications having the passed project user or the passed role as recipient.
     *
     * @param projectUser The required project user recipient
     * @param role The required role recipient
     * @return The list of found notifications
     */
    @Query(value = "select distinct on (n.id) n.* from {h-schema}t_notification n "
            + "left join {h-schema}ta_notification_role_name r on r.notification_id = n.id "
            + "left join {h-schema}ta_notification_projectuser_email e on e.notification_id = n.id "
            + "where n.status= ?1 and (e.projectuser_email = ?2 or r.role_name= ?3) "
            + "GROUP BY n.id ORDER BY ?#{#pageable}",
            countQuery = "select count(distinct n.id) from {h-schema}t_notification n "
                    + "left join {h-schema}ta_notification_role_name r on r.notification_id = n.id "
                    + "left join {h-schema}ta_notification_projectuser_email e on e.notification_id = n.id "
                    + "where n.status= ?1 and (e.projectuser_email = ?2 or r.role_name= ?3) "
                    + "GROUP BY n.id",
            nativeQuery = true)
    Page<Notification> findByStatusAndRecipientsContaining(String status, String projectUser, String role,
            Pageable page);

    /**
     * Find all notifications with passed <code>status</code>
     *
     * @param pStatus
     *            The notification status
     * @return The list of notifications
     */
    Page<Notification> findByStatus(NotificationStatus pStatus, Pageable page);

    /**
     * Find all notifications which recipients contains the given user, represented by its email
     * @param email
     * @return all notifications which recipients contains the given user, represented by its email
     */
    Page<Notification> findAllByProjectUserRecipientsContaining(String email, Pageable page);

    /**
     * Find all notifications which recipients contains the given role, represented by its name
     * @param role
     * @return all notifications which recipients contains the given role, represented by its name
     */
    Page<Notification> findAllByRoleRecipientsContaining(String role, Pageable page);

    @Modifying
    @Query(value = "UPDATE {h-schema}t_notification set status = ?1 FROM {h-schema}ta_notification_role_name recipient WHERE t_notification.id = recipient.notification_id AND recipient.role_name = ?2",
            nativeQuery = true)
    void updateAllNotificationStatusByRole(String status, String role);

    @Modifying
    @Query(value = "UPDATE {h-schema}t_notification set status = ?1 FROM {h-schema}ta_notification_projectuser_email recipient WHERE t_notification.id = recipient.notification_id AND recipient.projectuser_email = ?2",
            nativeQuery = true)
    void updateAllNotificationStatusByUser(String status, String projectUser);
}
