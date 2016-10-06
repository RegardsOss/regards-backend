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
import fr.cnes.regards.modules.accessRights.domain.Account;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.accessRights.domain.UserStatus;
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
    private AccountsClient accountsClient_;

    /**
     * Security JWT service
     */
    @Autowired
    private JWTService jwtService_;

    @Override
    public ProjectUser retreiveUser(String pName, String pScope) {
        final ProjectUser user = new ProjectUser();
        final Role role = new Role();
        role.setName(GATEWAY_ROLE);
        user.setRole(role);
        final Account newAccount = new Account();
        newAccount.setLogin(pName);
        newAccount.setEmail("user@regards.c-s.fr");
        user.setAccount(newAccount);
        return user;
    }

    @Override
    public UserStatus authenticate(String pName, String pPassword, String pScope) {
        LOG.info("Trying to authenticate user " + pName + " with password=" + pPassword + " for project " + pScope);

        UserStatus status = UserStatus.ACCESS_DENIED;
        jwtService_.injectToken(pScope, GATEWAY_ROLE);
        try {
            final HttpEntity<Boolean> results = accountsClient_.validatePassword(pName, pPassword);
            if (results.getBody()) {
                status = UserStatus.ACCESS_GRANTED;
            }
        } catch (final NoSuchElementException e) {
            LOG.error(e.getMessage(), e);
        }

        return status;

    }

}
