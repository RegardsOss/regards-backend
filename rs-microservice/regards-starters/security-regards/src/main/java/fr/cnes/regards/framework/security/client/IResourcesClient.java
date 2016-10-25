/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.client;

import java.util.List;

import feign.Feign;
import feign.RequestLine;
import feign.Target;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
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
     * Generate client
     *
     * @param pTarget
     *            Target to add informations in header like Autorization.
     * @return IResourcesClient
     * @since 1.0-SNAPSHOT
     */
    static IResourcesClient build(final Target<IResourcesClient> pTarget) {
        return Feign.builder() // Feign customization
                .encoder(new GsonEncoder()).decoder(new GsonDecoder()).target(pTarget);
    }

    /**
     *
     * Get all resources from the current microservice.
     *
     * @return List<ResourceMapping>
     * @since 1.0-SNAPSHOT
     */
    @RequestLine("GET /security/resources")
    List<ResourceMapping> getResources();

}