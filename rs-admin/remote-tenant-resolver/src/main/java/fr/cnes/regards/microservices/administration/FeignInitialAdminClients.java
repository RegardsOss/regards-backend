/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;

import com.netflix.config.ConfigurationManager;

import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.hystrix.HystrixFeign;
import fr.cnes.regards.client.core.TokenClientProvider;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;

/**
 *
 * Class FeignInitialAdminClients
 *
 * During the microservice starting process (spring context boot), the feign clients are badly initialized and don'find
 * the servers with the eureka registry. This class is used as a bean to initiate the needed clients to the
 * administration service during the microservice boot process. Here the feign clients are manually configured to access
 * the first instance found of microservice administration.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class FeignInitialAdminClients {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeignInitialAdminClients.class);

    /**
     * Initial Feign client to administration service to retrieve informations about projects
     */
    private IProjectsClient projectsClient;

    /**
     * Eureka discovery client
     */
    private final DiscoveryClient discoveryClient;

    @Value("${regards.microservice.admin.name}")
    private String adminMicroserviceName;

    public FeignInitialAdminClients(final DiscoveryClient pDiscoveryClient) {
        super();
        discoveryClient = pDiscoveryClient;

        final List<ServiceInstance> instances = discoveryClient.getInstances(adminMicroserviceName);
        if (instances.isEmpty()) {
            String errorMessage = "No administration instance found. Microservice cannot start.";
            LOGGER.error(errorMessage);
            throw new UnsupportedOperationException(errorMessage);
        }

        // Init hystrix configuration
        ConfigurationManager.getConfigInstance().setProperty("hystrix.command.default.execution.isolation.strategy",
                                                             "SEMAPHORE");
        ConfigurationManager.getConfigInstance()
                .setProperty("hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds", 60000);

        projectsClient = HystrixFeign.builder().contract(new SpringMvcContract()).encoder(new GsonEncoder())
                .decoder(new ResponseEntityDecoder(new GsonDecoder()))
                .target(new TokenClientProvider<>(IProjectsClient.class, instances.get(0).getUri().toString()));
    }

    public IProjectsClient getProjectsClient() {
        return projectsClient;
    }

    public void setProjectsClient(final IProjectsClient pProjectsClient) {
        projectsClient = pProjectsClient;
    }

}
