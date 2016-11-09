/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private static Map<String, Boolean> maintenanceMap = new ConcurrentHashMap<>();

    private MaintenanceManager() {
        // override of public because it's an utility class
    }

    public static Map<String, Boolean> getMaintenanceMap() {
        return maintenanceMap;
    }

<<<<<<< Updated upstream
=======
    public static Boolean getMaintenance(String pTenant) {
        Boolean b = maintenanceMap.get(pTenant);
        // if the tenant is unknown, it is not in maintenance
        if (b == null) {
            b = Boolean.FALSE;
        }
        return b;
    }

>>>>>>> Stashed changes
    public static void addTenant(String pTenant) {
        maintenanceMap.put(pTenant, Boolean.FALSE);
    }

    public static void setMaintenance(String pTenant) {
        maintenanceMap.put(pTenant, Boolean.TRUE);
    }

    public static void unSetMaintenance(String pTenant) {
        maintenanceMap.put(pTenant, Boolean.FALSE);
    }

}
