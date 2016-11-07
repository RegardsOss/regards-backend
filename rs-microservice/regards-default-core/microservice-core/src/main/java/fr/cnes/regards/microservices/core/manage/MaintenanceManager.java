/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.manage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 *
 * Class containing the knowledge about projects which are in maintenance
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Component
public final class MaintenanceManager {

    /**
     * map linking each tenant with his mode
     */
    private static Map<String, Boolean> maintenanceMap = new ConcurrentHashMap<>();

    private MaintenanceManager() {
        // override the default public constructor
    }

    public static Map<String, Boolean> getMaintenanceMap() {
        return maintenanceMap;
    }

    public static void setMaintenance(String pTenant) {
        if (pTenant == null) {
            maintenanceMap.put("INSTANCE", Boolean.TRUE);
        } else {
            maintenanceMap.put(pTenant, Boolean.TRUE);
        }
    }

    public static void unSetMaintenance(String pTenant) {
        maintenanceMap.put(pTenant, Boolean.FALSE);
    }

}
