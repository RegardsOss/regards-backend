/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.security.autoconfigure.endpoint.IMethodAuthorizationService;
import fr.cnes.regards.framework.security.autoconfigure.endpoint.ResourceMapping;

/**
 *
 * Class ResourcesController
 *
 * Common Resources RestController. This Controller manage the endpoint to retrieve all Resources of a microservice.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RestController
@RequestMapping("/")
public class ResourcesController {

    /**
     * Authorization service
     */
    private final IMethodAuthorizationService service;

    /**
     *
     * Constructor
     *
     * @param pService
     *            MethodeAutorizationService autowired by spring
     * @since 1.0-SNAPSHOT
     */
    public ResourcesController(final IMethodAuthorizationService pService) {
        service = pService;
    }

    /**
     *
     * Retrieve all enpoints annoted with @ResourceAccess
     *
     * @return List<ResourceMapping>
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(value = "resources", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<List<ResourceMapping>> getAllResources() {
        return new ResponseEntity<>(service.getResources(), HttpStatus.OK);
    }
}
