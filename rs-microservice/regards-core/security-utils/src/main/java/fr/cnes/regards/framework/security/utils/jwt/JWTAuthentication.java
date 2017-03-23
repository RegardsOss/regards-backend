/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils.jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;

/**
 *
 * REGARDS custom authentication.<br/>
 * All attributes of this class are filled from JWT content.
 *
 * @author msordi
 *
 */
public class JWTAuthentication implements Authentication {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * JWT from request header
     */
    private final String jwt;

    /**
     * Current user info
     */
    private UserDetails user;

    /**
     * Whether the user is authenticated
     */
    private Boolean isAuthenticated;

    /**
     * Constructor
     *
     * @param pJWT
     *            the JSON Web Token
     */
    public JWTAuthentication(String pJWT) {
        jwt = pJWT;
    }

    @Override
    public String getName() {
        return user.getName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<RoleAuthority> role = new ArrayList<>();
        role.add(new RoleAuthority(user.getRole()));
        return role;
    }

    @Override
    public Object getCredentials() {
        // JWT do not need credential
        return null;
    }

    @Override
    public Object getDetails() {
        return getPrincipal();
    }

    @Override
    public UserDetails getPrincipal() {
        return user;
    }

    @Override
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    @Override
    public void setAuthenticated(boolean pIsAuthenticated) throws IllegalArgumentException {
        isAuthenticated = pIsAuthenticated;
    }

    /**
     * Abstraction on how to get the tenant
     *
     * @return tenant for whom the JWT was provided
     */
    public String getTenant() {
        return user.getTenant();
    }

    /**
     * Abstraction on how to set the tenant
     *
     * @param pTenant
     *            the new tenant
     */
    public void setTenant(String pTenant) {
        user.setTenant(pTenant);
    }

    /**
     * @return the jwt
     */
    public String getJwt() {
        return jwt;
    }

    /**
     * Set user role
     *
     * @param pRoleName
     *            the role name
     */
    public void setRole(String pRoleName) {
        if (user == null) {
            throw new IllegalStateException("role cannot be set while user has not been set!");
        }
        user.setRole(pRoleName);
    }

    /**
     * @return the user
     */
    public UserDetails getUser() {
        return user;
    }

    /**
     * @param pUser
     *            the user to set
     */
    public void setUser(UserDetails pUser) {
        user = pUser;
    }
}
