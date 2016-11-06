/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.provider;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import fr.cnes.regards.cloud.gateway.authentication.plugins.AuthenticateStatus;
import fr.cnes.regards.cloud.gateway.authentication.plugins.IAuthenticationPlugin;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;

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
public class AuthenticationProviderStub implements IAuthenticationPlugin {

    /**
     * Invalid password for test
     */
    public static final String INVALID_PASSWORD = "invalid";

    @Override
    public AuthenticateStatus authenticate(final String pName, final String pPassword, final String pScope) {
        AuthenticateStatus status = AuthenticateStatus.ACCESS_DENIED;
        if (pPassword.equals(INVALID_PASSWORD)) {
            status = AuthenticateStatus.INVALID_PASSWORD;
        } else {
            status = AuthenticateStatus.ACCESS_GRANTED;
        }
        return status;
    }

    @Override
    public UserDetails retreiveUserDetails(final String pName, final String pScope) {
        final UserDetails user = new UserDetails();
        user.setRole("USER");
        user.setEmail("user@regards.c-s.fr");
        return user;
    }

}
