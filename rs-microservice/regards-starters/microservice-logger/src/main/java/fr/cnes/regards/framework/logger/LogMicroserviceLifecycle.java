/*

 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.framework.logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * Logger for microservice lifecycle(start/stop)
 *
 * @author Stephane Cortine
 **/
public class LogMicroserviceLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogMicroserviceLifecycle.class);

    private final Environment environment;

    public LogMicroserviceLifecycle(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void onStartup() {
        LOGGER.info(LogConstants.SECURITY_MARKER, "Starting microservice: {}", getMicroserviceName());
    }

    @PreDestroy
    public void onShutdown() {
        LOGGER.info(LogConstants.SECURITY_MARKER, "Stopping microservice: {}", getMicroserviceName());
    }

    private String getMicroserviceName() {
        return environment.getProperty("spring.application.name", "Inconnu");
    }
}
