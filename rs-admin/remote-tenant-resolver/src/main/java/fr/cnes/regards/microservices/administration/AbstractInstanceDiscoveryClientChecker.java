/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.microservices.administration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;

/**
 * Ensure a RS-ADMIN instance is available before storing or collecting multitenant information.
 *
 * @author Marc Sordi
 */
public abstract class AbstractInstanceDiscoveryClientChecker implements InitializingBean {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractInstanceDiscoveryClientChecker.class);

    /**
     * Admin instance id on EUREKA server
     */
    private static final String ADMIN_INSTANCE_ID = "rs-admin-instance";

    /**
     * Discovery client implemented with EUREKA client in this context
     */
    protected final DiscoveryClient discoveryClient;

    protected AbstractInstanceDiscoveryClientChecker(final DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    protected void checkAvailability(String instanceId) {
        final List<ServiceInstance> instances = discoveryClient.getInstances(instanceId);
        if (instances.isEmpty()) {
            String errorMessage = String.format("No instance of %s found. Microservice cannot start.", instanceId);
            LOGGER.error(errorMessage);
            throw new UnsupportedOperationException(errorMessage);
        }
    }

    @Override
    public void afterPropertiesSet() {
        checkAvailability(ADMIN_INSTANCE_ID);
    }

}
