/*
 * LICENSE_PLACEHOLDER
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

    public static ConcurrentMap<String, MaintenanceInfo> getMaintenanceMap() {
        return maintenanceMap;
    }

    public static Boolean getMaintenance(String pTenant) {
        MaintenanceInfo info = maintenanceMap.get(pTenant);
        // if the tenant is unknown, it is not in maintenance
        if (info == null) {
            return Boolean.FALSE;
        }
        return info.getActive();
    }

    public static void addTenant(String pTenant) {
        maintenanceMap.put(pTenant, new MaintenanceInfo(Boolean.FALSE, null));
    }

    public static void setMaintenance(String pTenant) {
        maintenanceMap.put(pTenant, new MaintenanceInfo(Boolean.TRUE, OffsetDateTime.now().withOffsetSameInstant(
                ZoneOffset.UTC)));
    }

    public static void unSetMaintenance(String pTenant) {
        maintenanceMap.put(pTenant, new MaintenanceInfo(Boolean.FALSE, OffsetDateTime.now().withOffsetSameInstant(
                ZoneOffset.UTC)));
    }

}
