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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import fr.cnes.regards.cloud.gateway.authentication.interfaces.IAuthenticationProvider;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.UserStatus;

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

    private static final Logger LOG = LoggerFactory.getLogger(Oauth2AuthenticationManager.class);

    /**
     * Custom authentication provider. TODO : Should be replace with the REGARDS Plugins system.
     */
    @Autowired
    private IAuthenticationProvider authProvider_;

    @Override
    public Authentication authenticate(Authentication pAuthentication) throws AuthenticationException {
        final String name = pAuthentication.getName();
        final String password = pAuthentication.getCredentials().toString();

        final Object details = pAuthentication.getDetails();
        String scope = null;
        if (details instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, String> detailsMap = (Map<String, String>) details;
            scope = detailsMap.get("scope");
        }
        else {
            final String message = "Invalid scope";
            LOG.error(message);
            throw new BadCredentialsException(message);
        }

        // Check user/password
        if (!authProvider_.authenticate(name, password, scope).equals(UserStatus.ACCESS_GRANTED)) {
            throw new BadCredentialsException("Access denied for user " + name);
        }

        // Retrieve account
        ProjectUser user = authProvider_.retreiveUser(name, scope);

        final List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority(user.getRole().getName()));

        return new UsernamePasswordAuthenticationToken(user, password, grantedAuths);

    }

}
