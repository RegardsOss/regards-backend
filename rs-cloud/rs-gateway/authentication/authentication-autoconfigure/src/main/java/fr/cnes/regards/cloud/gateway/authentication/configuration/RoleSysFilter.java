/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.configuration;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import fr.cnes.regards.framework.security.filter.IpFilter;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 *
 * Class RoleSysFilter
 *
 * Specific gateway Filter to deny access to all Systems Roles. Systems roles must be used between microservices only.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class RoleSysFilter extends OncePerRequestFilter {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(IpFilter.class);

    @Override
    protected void doFilterInternal(final HttpServletRequest pRequest, final HttpServletResponse pResponse,
            final FilterChain pFilterChain) throws ServletException, IOException {

        // Get authorized ip associated to given role
        final JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext()
                .getAuthentication();

        @SuppressWarnings("unchecked")
        final Collection<RoleAuthority> roles = (Collection<RoleAuthority>) authentication.getAuthorities();

        boolean isSysRole = false;
        if (!roles.isEmpty()) {
            for (final RoleAuthority role : roles) {
                if (RoleAuthority.isSysRole(role.getAuthority())) {
                    isSysRole = true;
                    final String message = "[REGARDS FILTER] - Authorization denied for SYS Roles";
                    LOG.error(message);
                    pResponse.sendError(HttpStatus.UNAUTHORIZED.value(), message);
                    break;
                }
            }
        }

        if (!isSysRole) {
            pFilterChain.doFilter(pRequest, pResponse);
        }

    }

}
