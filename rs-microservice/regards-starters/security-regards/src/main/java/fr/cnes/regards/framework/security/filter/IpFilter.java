/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 *
 * Class IPFilter
 *
 * Spring MVC request filter by IP
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class IpFilter extends GenericFilterBean {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(IpFilter.class);

    /**
     * Provider of authorities entities
     */
    private final IAuthoritiesProvider authoritiesProvider;

    /**
     *
     * Constructor
     *
     * @param pAuthoritiesProvider
     *            {@link IAuthoritiesProvider}
     * @since 1.0-SNAPSHOT
     */
    public IpFilter(final IAuthoritiesProvider pAuthoritiesProvider) {
        super();
        authoritiesProvider = pAuthoritiesProvider;
    }

    @Override
    public void doFilter(final ServletRequest pRequest, final ServletResponse pResponse, final FilterChain pFilterChain)
            throws IOException, ServletException {

        LOG.info(String.format("[REGARDS IP FILTER] Request sent from %s", pRequest.getRemoteAddr()));

        // Get authorized ip associated to given role
        final JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext()
                .getAuthentication();

        @SuppressWarnings("unchecked")
        final Collection<RoleAuthority> roles = (Collection<RoleAuthority>) authentication.getAuthorities();

        final List<String> authorizedAddresses = new ArrayList<>();
        if (!roles.isEmpty()) {
            for (final RoleAuthority role : roles) {
                authorizedAddresses.addAll(authoritiesProvider.getRoleAuthorizedAddress(role.getAuthority()));
            }

        } else {
            throw new InsufficientAuthenticationException("No role defined");
        }

        if (!authorizedAddresses.isEmpty()) {
            for (final String authorizedAddress : authorizedAddresses) {
                final Pattern pattern = Pattern.compile(authorizedAddress);
                final Matcher matcher = pattern.matcher(pRequest.getRemoteAddr());
                if (!matcher.matches()) {
                    throw new InsufficientAuthenticationException(
                            String.format("[REGARDS IP FILTER] - %s -Authorization denied", pRequest.getRemoteAddr()));
                }
            }

        }
        LOG.info(String.format("[REGARDS IP FILTER] - %s -Authorization granted", pRequest.getRemoteAddr()));

        // Continue the filtering chain
        pFilterChain.doFilter(pRequest, pResponse);

    }

}
