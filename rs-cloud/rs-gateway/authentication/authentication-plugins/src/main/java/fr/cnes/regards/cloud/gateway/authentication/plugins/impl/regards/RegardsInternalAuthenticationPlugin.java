/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins.impl.regards;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.cloud.gateway.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationPluginResponse;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.accessrights.client.IAccountsClient;

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
        id = "RegardsInternalAuthenticationPlugin", version = "1.0", contact = "regards@c-s.fr", licence = "GPL V3",
        owner = "CNES", url = "www.cnes.fr")
public class RegardsInternalAuthenticationPlugin implements IAuthenticationPlugin {

    /**
     * rs-admin microservice client for accounts
     */
    @Autowired
    private IAccountsClient accountsClient;

    @Override
    public AuthenticationPluginResponse authenticate(final String pEmail, final String pPassword, final String pScope) {

        Boolean accessGranted = false;
        String errorMessage = null;

        // Validate password as system
        FeignSecurityManager.asSystem();
        final ResponseEntity<Boolean> validateResponse = accountsClient.validatePassword(pEmail, pPassword);

        if (validateResponse.getStatusCode().equals(HttpStatus.OK)) {
            if (validateResponse.getBody()) {
                accessGranted = validateResponse.getBody();
            } else {
                errorMessage = String.format("[REMOTE ADMINISTRATION] - validatePassword - Accound %s doesn't exists",
                                             pEmail);
            }
        } else {
            errorMessage = String.format("[REMOTE ADMINISTRATION] - validatePassword - Accound %s doesn't exists",
                                         pEmail);
        }

        return new AuthenticationPluginResponse(accessGranted, pEmail, errorMessage);

    }

}
