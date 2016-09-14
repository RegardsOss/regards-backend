/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.auth;

import org.springframework.security.core.GrantedAuthority;

public class RoleAuthority implements GrantedAuthority {

    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private String autority;

    public RoleAuthority(String authority) {
        if (!authority.startsWith(ROLE_PREFIX)) {
            this.autority = ROLE_PREFIX + authority;
        }
        else {
            this.autority = authority;
        }
    }

    @Override
    public String getAuthority() {
        return autority;
    }

}
