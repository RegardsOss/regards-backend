/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign;

import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;

import feign.Feign;
import feign.Target;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;

/**
 * Helper class for building Feign client programmatically
 *
 * @author Marc Sordi
 *
 */
public final class FeignClientBuilder {

    private FeignClientBuilder() {
    }

    /**
     *
     * Generate client
     *
     * @param pTarget
     *            Target to add informations in header like Autorization.
     * @return IResourcesClient a client instance
     */
    public static <T> T build(final Target<T> pTarget) {
        return Feign.builder() // Feign customization
                .encoder(new GsonEncoder()).decoder(new ResponseEntityDecoder(new GsonDecoder()))
                .errorDecoder(new ClientErrorDecoder()).decode404().contract(new SpringMvcContract()).target(pTarget);
    }
}
