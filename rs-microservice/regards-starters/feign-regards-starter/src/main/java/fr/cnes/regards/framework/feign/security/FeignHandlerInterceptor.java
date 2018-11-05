package fr.cnes.regards.framework.feign.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Spring interceptor that reset {@link FeignSecurityManager} before the request is being handled by the controllers
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class FeignHandlerInterceptor extends HandlerInterceptorAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(FeignHandlerInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        LOG.debug("Reset FeignSecurityManager");
        FeignSecurityManager.reset();
        return super.preHandle(request, response, handler);
    }

}
