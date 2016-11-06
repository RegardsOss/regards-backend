/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.filters;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

/**
 *
 * Class ProxyLogFilter
 *
 * Zuul proxy filter to log request redirections.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class ProxyLogFilter extends ZuulFilter {

    /**
     * Class logger
     */
    private static final Logger LOG = Logger.getLogger(ProxyLogFilter.class);

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        final RequestContext ctx = RequestContext.getCurrentContext();
        final HttpServletRequest request = ctx.getRequest();

        LOG.info(String.format("Zuul Filter <%s> : %s request to %s", this.getClass().getName(), request.getMethod(),
                               request.getRequestURL()
                                       .toString()));

        return null;
    }

}
