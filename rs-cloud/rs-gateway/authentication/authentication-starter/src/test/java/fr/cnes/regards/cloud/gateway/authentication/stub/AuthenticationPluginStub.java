/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.stub;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import fr.cnes.regards.cloud.gateway.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationPluginResponse;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.AuthenticationStatus;

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
        AuthenticationStatus status = AuthenticationStatus.ACCESS_DENIED;
        if (pPassword.equals(INVALID_PASSWORD)) {
            status = AuthenticationStatus.INVALID_PASSWORD;
        } else {
            status = AuthenticationStatus.ACCESS_GRANTED;
        }
        return new AuthenticationPluginResponse(status, pName, null);
    }
}
