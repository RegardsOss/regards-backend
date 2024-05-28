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

import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMode;
import fr.cnes.regards.modules.notification.domain.NotificationLight;
import fr.cnes.regards.modules.notification.domain.dto.SearchNotificationParameters;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Custom Repository to search and delete notification without in memory pagination.
 * We do not use JPA Repository here to improve performances.
 *
 * @author Sébastien Binda
 **/
@Service
public class NotificationLightCustomNativeQueryRepository {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationLightCustomNativeQueryRepository.class);

    private final EntityManager entityManager;

    private final INotificationLightRepository notificationLightRepository;

    public NotificationLightCustomNativeQueryRepository(EntityManager entityManager,
                                                        INotificationLightRepository notificationLightRepository) {
        this.entityManager = entityManager;
        this.notificationLightRepository = notificationLightRepository;
    }

    /**
     * Build one condition for where query part
     */
    private String buildCondition(String whereQuery, @Nullable String tableName, String condition) {
        return String.format("%s %s %s%s",
                             whereQuery,
                             (!whereQuery.isEmpty() ? " AND " : ""),
                             (tableName != null ? tableName + "." : ""),
                             condition);
    }

    /**
     * Creates where condition in SQL query to search for notification with filters
     */
    private String buildWhereQuery(SearchNotificationParameters filters,
                                   @Nullable String user,
                                   @Nullable String tableName) {
        String whereQuery = "";
        // If user is not set, so we don't want to filter on user and role. This case is used by
        // notification-instance service module as no user or role are defined.
        if (user != null) {
            whereQuery = "( pu.projectuser_email = :user or role.role_name = :role )";
        }
        if (filters.getLevels() != null && !filters.getLevels().getValues().isEmpty()) {
            whereQuery = buildCondition(whereQuery, tableName, "type in :levels");
        }
        if (filters.getSenders() != null && !filters.getSenders().getValues().isEmpty()) {
            whereQuery = buildCondition(whereQuery, tableName, "sender in :senders");
        }
        if (filters.getStatus() != null && !filters.getStatus().getValues().isEmpty()) {
            whereQuery = buildCondition(whereQuery, tableName, "status in :status");
        }
        if (filters.getDates() != null) {
            if (filters.getDates().getAfter() != null) {
                whereQuery = buildCondition(whereQuery, tableName, "date > :dateAfter");
            }
            if (filters.getDates().getBefore() != null) {
                whereQuery = buildCondition(whereQuery, tableName, "date < :dateBefore");
            }
        }
        if (filters.getIds() != null && !filters.getIds().getValues().isEmpty()) {
            if (filters.getIds().getMode() == ValuesRestrictionMode.INCLUDE) {
                whereQuery = buildCondition(whereQuery, tableName, "id in :ids");
            } else {
                whereQuery = buildCondition(whereQuery, tableName, "id not in :ids");
            }
        }
        return whereQuery.isEmpty() ? "" : "where " + whereQuery;

    }

    /**
     * Update current query to add filters used
     */
    private void updateQueryParameters(SearchNotificationParameters filters, String user, String role, Query query) {
        if (user != null) {
            query.setParameter("user", user);
        }
        if (role != null) {
            query.setParameter("role", role);
        }
        if (filters.getLevels() != null && !filters.getLevels().getValues().isEmpty()) {
            query.setParameter("levels", filters.getLevels().getValues().stream().map(Enum::toString).toList());
        }
        if (filters.getSenders() != null && !filters.getSenders().getValues().isEmpty()) {
            query.setParameter("senders", filters.getSenders().getValues());
        }
        if (filters.getStatus() != null && !filters.getStatus().getValues().isEmpty()) {
            query.setParameter("status", filters.getStatus().getValues().stream().map(Enum::toString).toList());
        }

        if (filters.getDates() != null) {
            if (filters.getDates().getAfter() != null) {
                query.setParameter("dateAfter", filters.getDates().getAfter());
            }
            if (filters.getDates().getBefore() != null) {
                query.setParameter("dateBefore", filters.getDates().getBefore());
            }
        }
        if (filters.getIds() != null && !filters.getIds().getValues().isEmpty()) {
            query.setParameter("ids", filters.getIds().getValues());
        }
    }

    /**
     * Retrieve a notification light page matching filter
     */
    public Page<NotificationLight> findAll(SearchNotificationParameters filters,
                                           @Nullable String user,
                                           @Nullable String role,
                                           int page,
                                           int pageSize) {
        long start = System.currentTimeMillis();

        String selectQuery = "notif.id";
        String selectCountQuery = "count(distinct notif.id)";
        String fromQuery = "t_notification notif ";
        if (user != null && role != null) {
            fromQuery += "left join ta_notification_projectuser_email pu on notif.id=pu.notification_id "
                         + "left join ta_notification_role_name role on notif.id=role.notification_id ";
        }

        String whereQuery = buildWhereQuery(filters, user, "notif");
        String queryString = String.format("select %s from %s %s group by notif.id order by max(date) DESC",
                                           selectQuery,
                                           fromQuery,
                                           whereQuery);
        String countQueryString = String.format("select %s from %s %s", selectCountQuery, fromQuery, whereQuery);

        // First query, search distinct ids of notification matching search parameters.
        // Native query is built directly for performance improvement.
        // We tried previously tu use Specification by results to in memory pagination due to foreign keys associations.
        Query query = entityManager.createNativeQuery(queryString);
        updateQueryParameters(filters, user, role, query);
        query.setMaxResults(pageSize);
        query.setFirstResult(pageSize * page);
        List<Long> resultIds = query.getResultList();

        // Once ids are found we use JPA repository to find complete entities with associated table values.
        // Here search is optimized because we search only by ids.
        List<NotificationLight> results = notificationLightRepository.findAllByIdInOrderByDateDesc(resultIds);

        // We need a third request to handle pagination and calculate the total number of results with a count query.
        Query queryCount = entityManager.createNativeQuery(countQueryString);
        updateQueryParameters(filters, user, role, queryCount);
        int total = ((Number) queryCount.getSingleResult()).intValue();

        LOGGER.debug("{} NOTIFICATIONS found in {}ms", results.size(), System.currentTimeMillis() - start);
        return new PageImpl<>(results, PageRequest.of(page, pageSize), total);
    }

    /**
     * Delete all notification matching given filters
     */
    @Transactional
    public void deleteAll(SearchNotificationParameters filters, @Nullable String user, @Nullable String role) {
        long start = System.currentTimeMillis();
        String fromQuery = "t_notification notif ";
        if (user != null && role != null) {
            // If user and role are provided we need to join with role and user association tables
            fromQuery = "t_notification USING t_notification AS notif "
                        + "left join ta_notification_projectuser_email pu on notif.id=pu.notification_id "
                        + "left join ta_notification_role_name role on notif.id=role.notification_id ";
        }
        String whereQuery = buildWhereQuery(filters, user, "notif");
        if (user != null && role != null) {
            // If user and role are provided we need to join with role and user association tables
            whereQuery = buildCondition(whereQuery, "t_notification", "id = notif.id");
        }
        String deleteQueryString = String.format("delete from %s %s", fromQuery, whereQuery);
        Query query = entityManager.createNativeQuery(deleteQueryString, NotificationLight.class);
        updateQueryParameters(filters, user, role, query);
        // Execute query and get results
        int nbDeleted = query.executeUpdate();
        LOGGER.info("{} NOTIFICATIONS deleted in {}ms", nbDeleted, System.currentTimeMillis() - start);
    }

}
