/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * Ensure a RS-ADMIN instance is available before storing or collecting multitenant information.
 *
 * @author Marc Sordi
 *
 */
public abstract class AbstractDiscoveryClientChecker {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDiscoveryClientChecker.class);

    /**
     * Admin instance id on EUREKA server
     */
    private static final String ADMIN_INSTANCE_ID = "rs-admin";

    /**
     * Discovery client implemented with EUREKA client in this context
     */
    protected final DiscoveryClient discoveryClient;

    protected AbstractDiscoveryClientChecker(final DiscoveryClient discoveryClient) {
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

    @PostConstruct
    public void doCheck() {
        checkAvailability(ADMIN_INSTANCE_ID);
    }

}
