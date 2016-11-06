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
import org.springframework.security.authentication.AbstractAuthenticationToken;
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
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.service.PluginService;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

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

    @Autowired
    private PluginService pluginService;

    @Autowired
    private IAuthenticationPlugin defaultAuthenticationPlugin;

    @Override
    public Authentication authenticate(final Authentication pAuthentication) {

        AbstractAuthenticationToken token = null;

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

        // Get all availables authentication plugins
        final List<PluginConfiguration> pluginConfigurations = pluginService
                .getPluginConfigurationsByType(IAuthenticationPlugin.class);
        if (pluginConfigurations != null && !pluginConfigurations.isEmpty()) {
            for (final PluginConfiguration config : pluginConfigurations) {
                try {
                    token = doPluginAuthentication(pluginService.getPlugin(config.getId()), name, password, scope);
                } catch (final PluginUtilsException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        } else {
            // Use default REGARDS internal plugin
            token = doPluginAuthentication(defaultAuthenticationPlugin, name, password, scope);
        }

        return token;

    }

    /**
     *
     * Do authentication job with the given authentication plugin
     *
     * @param pPlugin
     *            IAuthenticationPlugin to use for authentication
     * @param pUserName
     *            user name
     * @param pUserPassword
     *            user password
     * @param pScope
     *            scope
     * @return AbstractAuthenticationToken
     */
    private AbstractAuthenticationToken doPluginAuthentication(IAuthenticationPlugin pPlugin, String pUserName,
            String pUserPassword, String pScope) {

        // Check user/password
        if (!pPlugin.authenticate(pUserName, pUserPassword, pScope).equals(AuthenticateStatus.ACCESS_GRANTED)) {
            throw new BadCredentialsException("Access denied for user " + pUserName);
        }

        // Retrieve account
        UserDetails userDetails;
        try {
            userDetails = pPlugin.retreiveUserDetails(pUserName, pScope);
        } catch (final UserNotFoundException e) {
            throw new BadCredentialsException(String.format("User %s does not exists ", pUserName));
        }

        final List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority(userDetails.getRole()));

        return new UsernamePasswordAuthenticationToken(userDetails, pUserPassword, grantedAuths);

    }

}
