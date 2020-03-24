/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
     * @param pException exception thrown
     * @return response
     */
    @ExceptionHandler(MaintenanceException.class)
    public ResponseEntity<ServerErrorResponse> handleMaintenanceException(MaintenanceException pException) {
        MaintenanceManager.setMaintenance(resolver.getTenant());
        LOGGER.error("Maintenance mode activated for tenant {}", resolver.getTenant());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ServerErrorResponse(pException.getMessage(), pException));
    }

    /**
     * Exception handler catching any exception that are not already handled
     * @param throwable exception thrown
     * @return response
     */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ServerErrorResponse> handleThrowable(Throwable throwable) {
        LOGGER.error("Unexpected server error", throwable);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ServerErrorResponse(throwable.getMessage(), throwable));
    }

}
