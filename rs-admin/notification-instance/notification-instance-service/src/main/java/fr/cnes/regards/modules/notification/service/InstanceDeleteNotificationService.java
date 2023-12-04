/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notification.service;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.modules.notification.dao.INotificationLightRepository;
import fr.cnes.regards.modules.notification.domain.NotificationLight;
import fr.cnes.regards.modules.notification.domain.dto.SearchNotificationParameters;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.util.stream.Collectors;

/**
 * Service to delete notifications
 *
 * @author Théo Lasserre
 */
@Service
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class InstanceDeleteNotificationService {

    private final IInstanceNotificationService notificationService;

    /**
     * CRUD repository managing light notifications
     */
    private final INotificationLightRepository notificationLightRepository;

    public InstanceDeleteNotificationService(IInstanceNotificationService notificationService,
                                             INotificationLightRepository notificationLightRepository) {
        this.notificationService = notificationService;
        this.notificationLightRepository = notificationLightRepository;
    }

    /**
     * Delete a notification light page matching filter
     *
     * @param filters  search parameters
     * @param pageable the paging information
     * @return a notification light page
     */
    @RegardsTransactional(propagation = Propagation.REQUIRES_NEW)
    public Page<NotificationLight> deleteNotificationWithFilter(SearchNotificationParameters filters,
                                                                Pageable pageable) {
        Page<NotificationLight> notificationLightPage = notificationService.findAll(filters, pageable);
        notificationLightRepository.deleteByIdIn(notificationLightPage.stream()
                                                                      .map(NotificationLight::getId)
                                                                      .collect(Collectors.toList()));
        return notificationLightPage;
    }
}
