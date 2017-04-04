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
    private String access_token; // NOSONAR: has this structure so we don't need a DTO or adapter for serialization

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

    public String getAccessToken() {
        return access_token;
    }

    public void setAccessToken(String pAccessToken) {
        access_token = pAccessToken;
    }

}
