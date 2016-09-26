/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.security.utils.jwt;

import java.io.Serializable;

/**
 * This object store REGARDS security principal<br/>
 * After request authentication, this object can be retrieved calling {@link JWTAuthentication#getPrincipal()}
 *
 * @author msordi
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
    private String name_;

    /**
     * User email
     */
    private String email_;

    /**
     * Tenant the user is requesting
     */
    private String tenant_;

    /**
     * @return the name
     */
    public String getName() {
        return name_;
    }

    /**
     * @param pName
     *            the name to set
     */
    public void setName(String pName) {
        name_ = pName;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email_;
    }

    /**
     * @param pEmail
     *            the email to set
     */
    public void setEmail(String pEmail) {
        email_ = pEmail;
    }

    /**
     * @return the tenant
     */
    public String getTenant() {
        return tenant_;
    }

    /**
     * @param pTenant
     *            the tenant to set
     */
    public void setTenant(String pTenant) {
        tenant_ = pTenant;
    }

}
