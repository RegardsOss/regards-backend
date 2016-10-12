/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.providers;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.cloud.gateway.authentication.interfaces.IAuthenticationProvider;
import fr.cnes.regards.modules.accessRights.client.AccountsClient;
import fr.cnes.regards.modules.accessRights.domain.UserStatus;
import fr.cnes.regards.modules.accessRights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.projects.Role;
import fr.cnes.regards.security.utils.jwt.JWTService;

/**
 *
 * Class SimpleAuthentication
 *
 * TODO : This class should be a REGARDS authentication plugin.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Component
public class SimpleAuthentication implements IAuthenticationProvider {

    /**
     * Gateway role
     */
    private static final String GATEWAY_ROLE = "USER";

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(SimpleAuthentication.class);

    /**
     * rs-admin microservice client for accounts
     */
    @Autowired
    private AccountsClient accountsClient;

    /**
     * Security JWT service
     */
    @Autowired
    private JWTService jwtService;

    @Override
    public ProjectUser retreiveUser(final String pName, final String pScope) {
        final ProjectUser user = new ProjectUser();
        final Role role = new Role();
        role.setName(GATEWAY_ROLE);
        user.setRole(role);
        user.setEmail("user@regards.c-s.fr");
        return user;
    }

    @Override
    public UserStatus authenticate(final String pName, final String pPassword, final String pScope) {
        LOG.info("Trying to authenticate user " + pName + " with password=" + pPassword + " for project " + pScope);

        UserStatus status = UserStatus.ACCESS_DENIED;
        jwtService.injectToken(pScope, GATEWAY_ROLE);
        try {
            final HttpEntity<Boolean> results = accountsClient.validatePassword(pName, pPassword);
            if (results.getBody()) {
                status = UserStatus.ACCESS_GRANTED;
            }
        } catch (final NoSuchElementException e) {
            LOG.error(e.getMessage(), e);
        }

        return status;

    }

}
