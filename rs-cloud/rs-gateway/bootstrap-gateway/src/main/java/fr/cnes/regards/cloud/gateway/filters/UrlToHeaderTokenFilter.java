/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.filters;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

/**
 *
 * This filter detects JWT in the URL query params and if found set it into the header.<br/>
 * Source URL query param : token<br/>
 * Target header : Bearer : Authorization
 *
 * @author Marc Sordi
 */
@Component
public class UrlToHeaderTokenFilter extends ZuulFilter {

    /**
     * Authorization header
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * Authorization header scheme
     */
    public static final String BEARER = "Bearer";

    /**
     * Token that may be passed through request query parameter for download purpose
     */
    public static final String TOKEN = "token";

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
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();

        // Try to retrieve JWT
        String jwt = request.getParameter(TOKEN);

        // Inject into header if not null and bearer not already set
        if ((jwt != null) && (request.getParameter(BEARER) == null)) {
            ctx.getZuulRequestHeaders().put(AUTHORIZATION, BEARER + " " + jwt);
        }

        return null;
    }

}
