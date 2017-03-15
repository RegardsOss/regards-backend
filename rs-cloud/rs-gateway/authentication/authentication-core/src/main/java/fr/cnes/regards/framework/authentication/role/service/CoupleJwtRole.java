/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.role.service;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class CoupleJwtRole {

    /**
     * Role for which the JWT was generated
     */
    private String role;

    /**
     * JWT to be used for authentication
     */
    private String access_token;

    public CoupleJwtRole(String pJwt, String pRoleName) {
        access_token = pJwt;
        role = pRoleName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String pRole) {
        role = pRole;
    }

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String pAccess_token) {
        access_token = pAccess_token;
    }

}
