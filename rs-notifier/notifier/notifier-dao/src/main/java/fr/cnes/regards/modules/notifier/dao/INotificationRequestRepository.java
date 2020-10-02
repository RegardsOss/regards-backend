/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.dao;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.notifier.domain.NotifRequestId;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import fr.cnes.regards.modules.notifier.domain.RecipientError;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;

/**
 * Repository to manipulate {@link NotificationRequest}
 * @author Kevin Marchois
 *
 */
@Repository
public interface INotificationRequestRepository extends JpaRepository<NotificationRequest, Long> {

    @Override
    @EntityGraph(attributePaths = {"recipientsScheduled", "recipientsInError", "recipientsToSchedule"})
    List<NotificationRequest> findAllById(Iterable<Long> ids);

    @EntityGraph(attributePaths = {"recipientsToSchedule", "recipientsScheduled"})
    Page<NotificationRequest> findByState(NotificationState state, Pageable pageable);

    /**
     * Update a state according a list of ids
     * @param state {@link NotificationState} to set
     * @param ids of {@link NotificationRequest}
     */
    @Modifying
    @Query("Update NotificationRequest notif set notif.state = :state Where notif.id in :ids")
    void updateState(@Param("state") NotificationState state, @Param("ids") Set<Long> ids);

    //TODO: check that hibernate is able to handle this request alone or if we should help it (pagination with join on other table
    @EntityGraph(attributePaths = {"recipientsToSchedule", "recipientsScheduled"})
    Page<NotificationRequest> findPageByStateAndRecipientsToScheduleContaining(NotificationState state,
            PluginConfiguration recipient, Pageable pageable);

    //TODO: check that hibernate is able to handle this request alone or if we should help it (pagination with join on other table
    // In query we check whether recipientsToSchedule is empty last because we consider that check success will only be called
    // after at least one recipient has been scheduled. So it is more likely that recipientsScheduled or recipientsInError
    // are not empty rather than recipientsToSchedule
    @Query("select nr from NotificationRequest nr where nr.state = :state and nr.recipientsScheduled is empty and"
            + " nr.recipientsInError is empty and nr.recipientsToSchedule is empty")
    Page<NotificationRequest> findByStateByRecipientsToScheduleEmptyByRecipientsScheduledEmptyByRecipientsInErrorEmpty(
            @Param("state") NotificationState state, Pageable pageable);

    Set<NotificationRequest> findAllByStateAndRequestIdIn(NotificationState state, Set<String> requestIds);
}
