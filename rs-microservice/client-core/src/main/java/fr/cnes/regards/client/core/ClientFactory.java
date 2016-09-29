/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.client.core;

import feign.Target;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.hystrix.HystrixFeign;
import feign.slf4j.Slf4jLogger;

/**
 *
 * Class ClientFactory
 *
 * Factory to build HystrixFeign client
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class ClientFactory {

    @SuppressWarnings("unchecked")
    public static <T> T build(Class<T> clazz, T fallback, String pUrl, String pJwtToken) {

        @SuppressWarnings("rawtypes")
        Target<T> target = new DynamicAuthTokenTarget(clazz, pUrl, pJwtToken);
        return new HystrixFeign.Builder().encoder(new GsonEncoder()).decoder(new GsonDecoder())
                .logger(new Slf4jLogger()).target(target, fallback);
    }

}
