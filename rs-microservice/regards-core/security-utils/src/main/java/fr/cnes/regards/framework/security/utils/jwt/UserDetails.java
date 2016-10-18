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
     * User email
     */
    private String email;

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
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param pEmail
     *            the email to set
     */
    public void setEmail(String pEmail) {
        email = pEmail;
    }

}
