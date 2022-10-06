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

public interface IOrderMaintenanceService {

    /**
     * Scheduled method to update all current running orders completions values and all order available files count
     * values into database
     */
    void updateCurrentOrdersComputations();

    /**
     * Same method as previous one but for one tenant (hence transactional)
     */
    void updateTenantOrdersComputations();

    /**
     * Scheduled method to search for orders whom available files counts haven't been updated since a specific delay
     */
    void sendPeriodicNotifications();

    /**
     * Same method as previous one but for one tenant (hence transactional)
     */
    void sendTenantPeriodicNotifications();

    /**
     * Search for expired orders then mark them as EXPIRED
     */
    void cleanExpiredOrders();

    void cleanExpiredOrdersForTenant();
}
