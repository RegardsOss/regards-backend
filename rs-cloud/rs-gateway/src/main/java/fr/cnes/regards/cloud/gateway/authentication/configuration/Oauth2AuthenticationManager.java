/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import fr.cnes.regards.cloud.gateway.authentication.interfaces.IAuthenticationProvider;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 *
 * Class Oauth2AuthenticationManager
 *
 * Authentication Manager. This class provide the authentication process to check user/password and retrieve user
 * account.
 *
 * @author CS
 * @since 1.0-SNPASHOT
 */
@Component
public class Oauth2AuthenticationManager implements AuthenticationManager {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Oauth2AuthenticationManager.class);

    /**
     * Custom authentication provider.
     */
    @Autowired
    private IAuthenticationProvider authProvider;

    @Override
    public Authentication authenticate(final Authentication pAuthentication) {
        final String name = pAuthentication.getName();
        final String password = pAuthentication.getCredentials().toString();

        final Object details = pAuthentication.getDetails();
        final String scope;
        if (details instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, String> detailsMap = (Map<String, String>) details;
            scope = detailsMap.get("scope");
        } else {
            final String message = "Invalid scope";
            LOG.error(message);
            throw new BadCredentialsException(message);
        }

        // Check user/password
        if (!authProvider.authenticate(name, password, scope).equals(UserStatus.ACCESS_GRANTED)) {
            throw new BadCredentialsException("Access denied for user " + name);
        }

        // Retrieve account
        final ProjectUser user = authProvider.retreiveUser(name, scope);

        final List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority(user.getRole().getName()));

        return new UsernamePasswordAuthenticationToken(user, password, grantedAuths);

    }

}
