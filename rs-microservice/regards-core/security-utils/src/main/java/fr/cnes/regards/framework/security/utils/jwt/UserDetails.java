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
     * User real name
     */
    private String name;

    /**
     * User role name
     */
    private String role;

    /**
     * Tenant the user is requesting
     */
    private String tenant;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param pName
     *            the name to set
     */
    public void setName(String pName) {
        name = pName;
    }

    /**
     * @return the tenant
     */
    public String getTenant() {
        return tenant;
    }

    /**
     * @param pTenant
     *            the tenant to set
     */
    public void setTenant(String pTenant) {
        tenant = pTenant;
    }

    /**
     *
     * @return the role name
     */
    public String getRole() {
        return role;
    }

    /**
     *
     * @param role
     *            the role name
     */
    public void setRole(String role) {
        this.role = role;
    }

}
