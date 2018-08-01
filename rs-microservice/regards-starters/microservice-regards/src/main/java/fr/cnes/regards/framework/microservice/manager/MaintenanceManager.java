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
package fr.cnes.regards.framework.microservice.manager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * Class containing the knowledge about projects which are in maintenance
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public final class MaintenanceManager {

    /**
     * map linking each tenant with his mode
     */
    private static ConcurrentMap<String, MaintenanceInfo> maintenanceMap = new ConcurrentHashMap<>();

    private MaintenanceManager() {
        // override of public because it's an utility class
    }

    /**
     * @return the maintenance map
     */
    public static ConcurrentMap<String, MaintenanceInfo> getMaintenanceMap() {
        return maintenanceMap;
    }

    /**
     * @param pTenant
     * @return whether the given tenant is in maintenance or not
     */
    public static Boolean getMaintenance(String pTenant) {
        MaintenanceInfo info = maintenanceMap.get(pTenant);
        // if the tenant is unknown, it is not in maintenance
        if (info == null) {
            return Boolean.FALSE;
        }
        return info.getActive();
    }

    /**
     * Add a tenant to the maintenance manager
     * @param pTenant
     */
    public static void addTenant(String pTenant) {
        maintenanceMap.put(pTenant, new MaintenanceInfo(Boolean.FALSE, OffsetDateTime.now()));
    }

    /**
     * Set the tenant in maintenance mode
     * @param pTenant
     */
    public static void setMaintenance(String pTenant) {
        maintenanceMap.put(pTenant, new MaintenanceInfo(Boolean.TRUE, OffsetDateTime.now().withOffsetSameInstant(
                ZoneOffset.UTC)));
    }

    /**
     * Set the tenant not in maintenance mode
     * @param pTenant
     */
    public static void unSetMaintenance(String pTenant) {
        maintenanceMap.put(pTenant, new MaintenanceInfo(Boolean.FALSE, OffsetDateTime.now().withOffsetSameInstant(
                ZoneOffset.UTC)));
    }

}
