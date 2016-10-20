/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils.endpoint;

import org.springframework.security.core.GrantedAuthority;

/**
 * REGARDS authority
 *
 * @author msordi
 *
 */
public class RoleAuthority implements GrantedAuthority {

    /**
     * Role prefix
     */
    private static final String ROLE_PREFIX = "ROLE_";

    private static final long serialVersionUID = 1L;

    /**
     * Role name prefixed with {@link #ROLE_PREFIX}
     */
    private String autority;

    public RoleAuthority(String pAuthority) {
        if (!pAuthority.startsWith(ROLE_PREFIX)) {
            this.autority = ROLE_PREFIX + pAuthority;
        } else {
            this.autority = pAuthority;
        }
    }

    @Override
    public String getAuthority() {
        return autority;
    }

    @Override
    public boolean equals(Object pObj) {
        if ((pObj != null) && (pObj instanceof RoleAuthority)) {
            return autority.equals(((RoleAuthority) pObj).getAuthority());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return autority.hashCode();
    }

}
