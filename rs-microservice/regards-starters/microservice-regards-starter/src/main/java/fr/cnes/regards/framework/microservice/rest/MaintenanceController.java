/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.rest;

import java.util.Map;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.microservice.manager.MaintenanceInfo;
import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;

/**
 * API REST allowing to manually handle maintenances
 *
 * TODO: verification on pTenant(it's one of the project or instance)
 *
 * @author Sylvain Vissiere-Guerinet
 * @since 1.0
 */
@RestController
@RequestMapping(MaintenanceController.MAINTENANCE_URL)
public class MaintenanceController {

    public static final String ENABLE = "enable";

    public static final String DISABLE = "disable";

    public static final String MAINTENANCE_URL = "/maintenance";

    public static final String MAINTENANCE_ACTIVATE_URL = "/{tenant}/" + ENABLE;

    public static final String MAINTENANCE_DESACTIVATE_URL = "/{tenant}/" + DISABLE;

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the map (tenant, maintenance) for this instance")
    public HttpEntity<Resource<Map<String, MaintenanceInfo>>> retrieveTenantsInMaintenance() {
        final Map<String, MaintenanceInfo> maintenaceMap = MaintenanceManager.getMaintenanceMap();
        return new ResponseEntity<>(new Resource<>(maintenaceMap), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, value = MAINTENANCE_ACTIVATE_URL)
    @ResourceAccess(description = "set this tenant into maintenance mode")
    public HttpEntity<Resource<Void>> setMaintenance(@PathVariable("tenant") String pTenant) {
        MaintenanceManager.setMaintenance(pTenant);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, value = MAINTENANCE_DESACTIVATE_URL)
    @ResourceAccess(description = "unset this tenant from maintenance mode")
    public HttpEntity<Resource<Void>> unSetMaintenance(@PathVariable("tenant") String pTenant) {
        MaintenanceManager.unSetMaintenance(pTenant);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
