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
package fr.cnes.regards.modules.order.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;

/**
 * Order job service.
 * This service is responsible of managing order jobs taken into account user restrictions on priorities and number of
 * concurrent jobs
 *
 * @author oroussel
 */
public interface IOrderJobService {

    void handleApplicationReadyEvent(ApplicationReadyEvent event);

    void handleRefreshScopeRefreshedEvent(RefreshScopeRefreshedEvent event);

    /**
     * Compute priority for next order/jobInfo(s)
     *
     * @param user user concerned by priority computing
     * @param role user role
     * @return a number between 0 and 100
     */
    int computePriority(String user, String role);

    /**
     * Manage user order jobs ie look if restriction on user concurrent jobs count permits new ones to be added and add
     * them if it is the case
     */
    void manageUserOrderStorageFilesJobInfos(String user);

    /**
     * Check if the given order is paused
     */
    boolean isOrderPaused(Long orderId);
}
