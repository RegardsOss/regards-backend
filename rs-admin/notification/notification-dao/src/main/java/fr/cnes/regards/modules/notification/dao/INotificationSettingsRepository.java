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

import fr.cnes.regards.modules.notification.domain.NotificationFrequency;
import fr.cnes.regards.modules.notification.domain.NotificationSettings;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.stream.Stream;

/**
 * Interface for an JPA auto-generated CRUD repository managing NotificationSettings.<br>
 * Embeds paging/sorting abilities by entending {@link PagingAndSortingRepository}.<br>
 * Allows execution of Query by Example {@link Example} instances.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface INotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    /**
     * Find the notification settings for the passed project user.
     *
     * @param userEmail The project user represented by its email
     * @return The found notification settings
     */
    NotificationSettings findOneByProjectUserEmail(String userEmail);

    /**
     * Retrieve all notification configuration parameters with passed frequency
     *
     * @param pFrequency The notification settings
     * @return The {@link NotificationSettings}
     */
    Stream<NotificationSettings> findByFrequency(NotificationFrequency pFrequency);
}
