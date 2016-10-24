/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils.client;

import org.springframework.security.core.context.SecurityContextHolder;

import feign.Request;
import feign.RequestTemplate;
import feign.Target;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 *
 * Class TokenClientProvider
 *
 * Feign client token provider. Add the JWT Token from the security context to the client requests.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class TokenClientProvider<T> implements Target<T> {

    /**
     * Feign client server url.
     */
    private final String url;

    /**
     * Feign client interface
     */
    private final Class<T> clazz;

    /**
     *
     * Constructor
     *
     * @param pClass
     *            interface
     * @param pUrl
     *            url
     * @since 1.0-SNAPSHOT
     */
    public TokenClientProvider(final Class<T> pClass, final String pUrl) {
        url = pUrl;
        clazz = pClass;
    }

    @Override
    public Request apply(final RequestTemplate input) {
        if (input.url().indexOf("http") != 0) {
            input.insert(0, url);
        }
        final JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext()
                .getAuthentication();

        input.header("Authorization", "Bearer " + authentication.getJwt());

        return input.request();
    }

    @Override
    public String name() {
        return "resources";
    }

    @Override
    public Class<T> type() {
        return clazz;
    }

    @Override
    public String url() {
        return url;
    }

}
