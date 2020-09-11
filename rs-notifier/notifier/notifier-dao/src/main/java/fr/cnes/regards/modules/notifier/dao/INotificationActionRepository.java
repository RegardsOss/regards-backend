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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
public interface INotificationActionRepository extends JpaRepository<NotificationRequest, Long> {

    Page<NotifRequestId> findByState(NotificationState state, Pageable pageable);

    /**
     * Update a state according a list of ids
     * @param state {@link NotificationState} to set
     * @param ids of {@link NotificationRequest}
     */
    @Modifying
    @Query("Update NotificationRequest notif set notif.state = :state Where notif.id in :ids")
    public void updateState(@Param("state") NotificationState state, @Param("ids") List<Long> ids);

    /**
     * Delete {@link NotificationRequest} without {@link RecipientError}
     */
    @Modifying
    @Query("Delete From NotificationRequest notif where notif not in (Select error.notification From RecipientError error)")
    public void deleteNoticationWithoutErrors();
}
