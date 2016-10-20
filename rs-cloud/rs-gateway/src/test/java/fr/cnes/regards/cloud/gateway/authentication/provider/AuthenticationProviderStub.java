/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.provider;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import fr.cnes.regards.cloud.gateway.authentication.interfaces.IAuthenticationProvider;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 *
 * Class AuthenticationProviderStub
 *
 * Stub to test for authentication and gettin JwtToken
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Component
@Primary
public class AuthenticationProviderStub implements IAuthenticationProvider {

    /**
     * Invalid password for test
     */
    public static final String INVALID_PASSWORD = "invalid";

    @Override
    public UserStatus authenticate(final String pName, final String pPassword, final String pScope) {
        UserStatus status = UserStatus.ACCESS_DENIED;
        if (!pPassword.equals(INVALID_PASSWORD)) {
            status = UserStatus.ACCESS_GRANTED;
        }
        return status;
    }

    @Override
    public ProjectUser retreiveUser(final String pName, final String pScope) {
        final ProjectUser user = new ProjectUser();
        final Role role = new Role();
        role.setName("USER");
        user.setRole(role);
        user.setEmail("user@regards.c-s.fr");
        return user;
    }

}
