/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.client;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.domain.ResourceMapping;

/**
 *
 * Class IResourcesClient
 *
 * Feign Client to access /security/resources common microservice endpoint
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@FunctionalInterface
public interface IResourcesClient {

    /**
     *
     * Get all resources from the current microservice.
     *
     * @return List<ResourceMapping>
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping(value = "/security/resources", method = RequestMethod.GET)
    List<ResourceMapping> getResources();

}