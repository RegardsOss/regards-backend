/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import fr.cnes.regards.framework.security.utils.HttpConstants;

/**
 * This class inject token in the {@link RequestTemplate}
 *
 * @author Marc Sordi
 *
 */
public class FeignSecurityInterceptor implements RequestInterceptor {

    /**
     * Feign security manager
     */
    protected final FeignSecurityManager feignSecurityManager;

    public FeignSecurityInterceptor(FeignSecurityManager pFeignSecurityManager) {
        this.feignSecurityManager = pFeignSecurityManager;
    }

    @Override
    public void apply(RequestTemplate pTemplate) {
        pTemplate.header(HttpConstants.AUTHORIZATION, HttpConstants.BEARER + " " + feignSecurityManager.getToken());
    }

}
