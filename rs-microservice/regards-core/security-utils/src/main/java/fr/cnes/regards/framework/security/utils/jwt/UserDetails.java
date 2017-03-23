/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils.jwt;

import java.io.Serializable;

/**
 * This object store REGARDS security principal<br/>
 * After request authentication, this object can be retrieved calling {@link JWTAuthentication#getPrincipal()}
 *
 * @author msordi
 * @author Christophe Mertz
 *
 */
public class UserDetails implements Serializable {

    /**
     * serialVersionUID field.
     */
    private static final long serialVersionUID = 4616778806989554358L;

    /**
     * Tenant the user is requesting
     */
    private String tenant;

    /**
     * User email
     */
    private String name;

    /**
     * User role name
     */
    private String role;

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String pTenant) {
        tenant = pTenant;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String pRole) {
        role = pRole;
    }

}
