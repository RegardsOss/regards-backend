/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.cloud.gateway.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationPluginResponse;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationStatus;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.accessrights.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.plugins.annotations.Plugin;

/**
 *
 * Class SimpleAuthentication
 *
 * Regards internal authentication plugin
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Plugin(author = "CSSI", description = "Regards internal authentication plugin",
        id = "RegardsInternalAuthenticationPlugin", version = "1.0")
public class RegardsInternalAuthenticationPlugin implements IAuthenticationPlugin {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RegardsInternalAuthenticationPlugin.class);

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * rs-admin microservice client for accounts
     */
    @Autowired
    private IAccountsClient accountsClient;

    /**
     * Security JWT service
     */
    @Autowired
    private JWTService jwtService;

    @Override
    public AuthenticationPluginResponse authenticate(final String pEmail, final String pPassword, final String pScope) {
        LOG.info("Trying to authenticate user " + pEmail + " with password=" + pPassword + " for project " + pScope);

        AuthenticationStatus status = AuthenticationStatus.ACCESS_DENIED;
        String errorMessage = null;

        try {
            jwtService.injectToken(pScope, RoleAuthority.getSysRole(microserviceName));
            final ResponseEntity<AccountStatus> validateResponse = accountsClient.validatePassword(pEmail, pPassword);
            if (validateResponse.getStatusCode().equals(HttpStatus.OK)) {
                if (validateResponse.getBody().equals(AccountStatus.ACTIVE)) {
                    status = AuthenticationStatus.ACCESS_GRANTED;
                } else {
                    status = AuthenticationStatus.ACCESS_DENIED;
                    errorMessage = "[REMOTE ADMINISTRATION] - validatePassword - Authentication failed.";
                }
            } else
                if (validateResponse.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                    status = AuthenticationStatus.ACCOUNT_NOT_FOUD;
                    errorMessage = String
                            .format("[REMOTE ADMINISTRATION] - validatePassword - Accound %s doesn't exists", pEmail);
                } else {
                    status = AuthenticationStatus.ACCESS_DENIED;
                    errorMessage = "[REMOTE ADMINISTRATION] - validatePassword - Request error code : "
                            + validateResponse.getStatusCode().toString();
                }
        } catch (final JwtException e) {
            LOG.error(e.getMessage(), e);
        }

        return new AuthenticationPluginResponse(status, pEmail, errorMessage);

    }

}
