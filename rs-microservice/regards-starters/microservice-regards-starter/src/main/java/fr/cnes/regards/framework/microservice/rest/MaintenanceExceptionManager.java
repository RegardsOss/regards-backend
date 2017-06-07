/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fr.cnes.regards.framework.microservice.maintenance.MaintenanceException;
import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.module.rest.representation.ServerErrorResponse;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.LOWEST_PRECEDENCE)
public class MaintenanceExceptionManager {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceExceptionManager.class);

    /**
     * Tenant resolver
     */
    @Autowired
    private IRuntimeTenantResolver resolver;

    /**
     * Exception handler catching {@link MaintenanceException} that are not already handled
     *
     * @param pException
     *            exception thrown
     * @return response
     */
    @ExceptionHandler(MaintenanceException.class)
    public ResponseEntity<ServerErrorResponse> handleMaintenanceException(MaintenanceException pException) {
        MaintenanceManager.setMaintenance(resolver.getTenant());
        LOGGER.error("Maintenance mode activated for tenant {}", resolver.getTenant());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ServerErrorResponse(pException.getMessage()));
    }

    /**
     * Exception handler catching any exception that are not already handled
     *
     * @param throwable
     *            exception thrown
     * @return response
     */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ServerErrorResponse> handleThrowable(Throwable throwable) {
        LOGGER.error("Unexpected server error", throwable);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ServerErrorResponse(throwable.getMessage()));
    }

}
