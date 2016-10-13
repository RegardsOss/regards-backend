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
    private final String jwt_;

    /**
     * List contains a single role
     */
    private List<RoleAuthority> roles_;

    /**
     * Current user info
     */
    private UserDetails user_;

    /**
     * Whether the user is authenticated
     */
    private Boolean isAuthenticated_;

    /**
     * Current tenant represented by the project
     */
    private String project_;

    /**
     * Constructor
     *
     * @param pJWT
     *            the JSON Web Token
     */
    public JWTAuthentication(String pJWT) {
        jwt_ = pJWT;
    }

    @Override
    public String getName() {
        return user_.getName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles_;
    }

    @Override
    public Object getCredentials() {
        // JWT do not need credential
        return null;
    }

    @Override
    public Object getDetails() {
        // Not used at the moment
        return null;
    }

    @Override
    public UserDetails getPrincipal() {
        return user_;
    }

    @Override
    public boolean isAuthenticated() {
        return isAuthenticated_;
    }

    @Override
    public void setAuthenticated(boolean pIsAuthenticated) throws IllegalArgumentException {
        isAuthenticated_ = pIsAuthenticated;
    }

    /**
     * @return the jwt
     */
    public String getJwt() {
        return jwt_;
    }

    /**
     * Set user role
     *
     * @param pRoleName
     *            the role name
     */
    public void setRole(String pRoleName) {
        if (roles_ == null) {
            roles_ = new ArrayList<>();
        }
        roles_.clear();
        roles_.add(new RoleAuthority(pRoleName));
    }

    /**
     * @return the user
     */
    public UserDetails getUser() {
        return user_;
    }

    /**
     * @param pUser
     *            the user to set
     */
    public void setUser(UserDetails pUser) {
        user_ = pUser;
    }

    /**
     * @return the project
     */
    public String getProject() {
        return project_;
    }

    /**
     * @param pProject
     *            the project to set
     */
    public void setProject(String pProject) {
        project_ = pProject;
    }
}
