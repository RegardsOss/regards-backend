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

import fr.cnes.regards.cloud.gateway.authentication.plugins.AuthenticateStatus;
import fr.cnes.regards.cloud.gateway.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.cloud.gateway.authentication.plugins.UserNotFoundException;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;

/**
 *
 * Class Oauth2AuthenticationManager
 *
 * Authentication Manager. This class provide the authentication process to check user/password and retrieve user
 * account.
 *
 * @author Sébastien Binda
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
    private IAuthenticationPlugin authProvider;

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
        if (!authProvider.authenticate(name, password, scope).equals(AuthenticateStatus.ACCESS_GRANTED)) {
            throw new BadCredentialsException("Access denied for user " + name);
        }

        // Retrieve account
        UserDetails userDetails;
        try {
            userDetails = authProvider.retreiveUserDetails(name, scope);
        } catch (final UserNotFoundException e) {
            throw new BadCredentialsException(String.format("User %s does not exists ", name));
        }

        final List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority(userDetails.getRole()));

        return new UsernamePasswordAuthenticationToken(userDetails, password, grantedAuths);

    }

}
