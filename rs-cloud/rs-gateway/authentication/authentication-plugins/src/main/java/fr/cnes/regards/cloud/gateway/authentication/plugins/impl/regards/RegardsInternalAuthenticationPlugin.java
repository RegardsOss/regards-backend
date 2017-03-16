/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins.impl.regards;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.cloud.gateway.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationPluginResponse;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationStatus;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.accessrights.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;

/**
 *
 * Class SimpleAuthentication
 *
 * Regards internal authentication plugin
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Plugin(author = "CSSI", description = "Regards internal authentication plugin", id = "RegardsInternalAuthenticationPlugin", version = "1.0")
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

    @Value("${regards.accounts.root.user.login}")
    private String rootLogin;

    /**
     * rs-admin microservice client for accounts
     */
    @Autowired
    private IAccountsClient accountsClient;

    @Override
    public AuthenticationPluginResponse authenticate(final String pEmail, final String pPassword, final String pScope) {

        AuthenticationStatus status = AuthenticationStatus.ACCESS_DENIED;
        String errorMessage = null;

        // Validate password as system
        FeignSecurityManager.asSystem();
        final ResponseEntity<AccountStatus> validateResponse = accountsClient.validatePassword(pEmail, pPassword);

        if (validateResponse.getStatusCode().equals(HttpStatus.OK)) {
            if (validateResponse.getBody().equals(AccountStatus.ACTIVE)) {
                status = AuthenticationStatus.ACCESS_GRANTED;
            } else {
                status = AuthenticationStatus.ACCESS_DENIED;
                errorMessage = "[REMOTE ADMINISTRATION] - validatePassword - Authentication failed.";
            }
        } else if (validateResponse.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
            status = AuthenticationStatus.ACCOUNT_NOT_FOUND;
            errorMessage = String.format("[REMOTE ADMINISTRATION] - validatePassword - Accound %s doesn't exists",
                                         pEmail);
        } else {
            status = AuthenticationStatus.ACCESS_DENIED;
            errorMessage = "[REMOTE ADMINISTRATION] - validatePassword - Request error code : "
                    + validateResponse.getStatusCode().toString();
        }

        return new AuthenticationPluginResponse(status, pEmail, errorMessage, rootLogin.equals(pEmail));

    }

}
