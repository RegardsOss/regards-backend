/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.stub;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import fr.cnes.regards.cloud.gateway.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationPluginResponse;

/**
 *
 * Class AuthenticationProviderStub
 *
 * Stub to test for authentication and gettin JwtToken
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Component
@Primary
public class AuthenticationPluginStub implements IAuthenticationPlugin {

    /**
     * Invalid password for test
     */
    public static final String INVALID_PASSWORD = "invalid";

    @Override
    public AuthenticationPluginResponse authenticate(final String pName, final String pPassword, final String pScope) {
        Boolean accessGranted = true;
        if (pPassword.equals(INVALID_PASSWORD)) {
            accessGranted = false;
        }
        return new AuthenticationPluginResponse(accessGranted, pName, null);
    }
}
