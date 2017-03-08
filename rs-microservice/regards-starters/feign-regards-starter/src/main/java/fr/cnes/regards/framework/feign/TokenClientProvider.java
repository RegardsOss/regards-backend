/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign;

import feign.Request;
import feign.RequestTemplate;
import feign.Target;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.security.utils.HttpConstants;

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
     * Feign security manager
     */
    private final FeignSecurityManager feignSecurityManager;

    public TokenClientProvider(final Class<T> pClass, final String pUrl, FeignSecurityManager pFeignSecurityManager) {
        url = pUrl;
        clazz = pClass;
        this.feignSecurityManager = pFeignSecurityManager;
    }

    @Override
    public Request apply(final RequestTemplate pTemplate) {
        if (pTemplate.url().indexOf("http") != 0) {
            pTemplate.insert(0, url);
        }
        // Apply security
        pTemplate.header(HttpConstants.AUTHORIZATION, HttpConstants.BEARER + " " + feignSecurityManager.getToken());
        return pTemplate.request();
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
