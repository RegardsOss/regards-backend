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

    private static final String ROLE_PREFIX = "ROLE_";

    private static final long serialVersionUID = 1L;

    private String autority_;

    public RoleAuthority(String pAuthority) {
        if (!pAuthority.startsWith(ROLE_PREFIX)) {
            this.autority_ = ROLE_PREFIX + pAuthority;
        } else {
            this.autority_ = pAuthority;
        }
    }

    @Override
    public String getAuthority() {
        return autority_;
    }

    @Override
    public boolean equals(Object pObj) {
        if ((pObj != null) && (pObj instanceof RoleAuthority)) {
            return autority_.equals(((RoleAuthority) pObj).getAuthority());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return autority_.hashCode();
    }

}
