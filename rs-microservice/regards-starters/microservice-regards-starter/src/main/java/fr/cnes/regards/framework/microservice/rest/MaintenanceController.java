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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
public class MaintenanceController {

    public static final String MAINTENANCES_URL = "/maintenances";

    public static final String MAINTENANCES_ACTIVATE_URL = MAINTENANCES_URL + "/{tenant}/activate";

    public static final String MAINTENANCES_DESACTIVATE_URL = MAINTENANCES_URL + "/{tenant}/desactivate";

    @RequestMapping(method = RequestMethod.GET, value = MAINTENANCES_URL, produces = "application/json")
    @ResourceAccess(description = "retrieve the map (tenant, maintenance) for this instance")
    @ResponseBody
    public HttpEntity<Resource<Map<String, Boolean>>> retrieveTenantsInMaintenance() {
        final Map<String, Boolean> maintenaceMap = MaintenanceManager.getMaintenanceMap();
        final Resource<Map<String, Boolean>> resource = new Resource<>(maintenaceMap);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, value = MAINTENANCES_ACTIVATE_URL, produces = "application/json")
    @ResourceAccess(description = "set this tenant into maintenance mode")
    @ResponseBody
    public HttpEntity<Resource<Void>> setMaintenance(@PathVariable("tenant") String pTenant) {
        MaintenanceManager.setMaintenance(pTenant);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, value = MAINTENANCES_DESACTIVATE_URL, produces = "application/json")
    @ResourceAccess(description = "unset this tenant from maintenance mode")
    @ResponseBody
    public HttpEntity<Resource<Void>> unSetMaintenance(@PathVariable("tenant") String pTenant) {
        MaintenanceManager.unSetMaintenance(pTenant);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
