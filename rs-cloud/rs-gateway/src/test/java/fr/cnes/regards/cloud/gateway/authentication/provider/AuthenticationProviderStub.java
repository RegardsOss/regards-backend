/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.provider;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import fr.cnes.regards.cloud.gateway.authentication.interfaces.IAuthenticationProvider;
import fr.cnes.regards.modules.accessRights.domain.Account;
import fr.cnes.regards.modules.accessRights.domain.ProjectUser;
import fr.cnes.regards.modules.accessRights.domain.Role;
import fr.cnes.regards.modules.accessRights.domain.UserStatus;

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

    @Override
    public UserStatus authenticate(String pName, String pPassword, String pScope) {
        return UserStatus.ACCESS_GRANTED;
    }

    @Override
    public ProjectUser retreiveUser(String pName, String pScope) {
        ProjectUser user = new ProjectUser();
        Role role = new Role();
        role.setName("USER");
        user.setRole(role);
        Account newAccount = new Account();
        newAccount.setEmail("user@regards.c-s.fr");
        user.setAccount(newAccount);
        return user;
    }

}
