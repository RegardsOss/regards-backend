/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.security.utils.jwt;

import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.*;

/**
 * REGARDS custom authentication.<br/>
 * All attributes of this class are filled from JWT content.
 *
 * @author msordi
 */
public class JWTAuthentication implements Authentication {
    /**
     * JWT from request header
     */
    private final String jwt;

    /**
     * Current tenant
     */
    private String tenant;

    /**
     * Current user info
     */
    private UserDetails user;

    /**
     * Whether the user is authenticated
     */
    private Boolean isAuthenticated;

    /**
     * Additional parameters (user specific)
     */
    private Map<String, Object> additionalParams = new HashMap<>();

    /**
     * Constructor
     *
     * @param jwt the JSON Web Token
     */
    public JWTAuthentication(String jwt) {
        this.jwt = jwt;
    }

    @Override
    public String getName() {
        return user.getEmail();
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
    public void setAuthenticated(boolean authenticated) throws IllegalArgumentException {
        isAuthenticated = authenticated;
    }

    /**
     * Abstraction on how to get the tenant
     *
     * @return tenant for whom the JWT was provided
     */
    public String getTenant() {
        return Optional.ofNullable(user).map(UserDetails::getTenant).orElse(tenant);
    }

    /**
     * Abstraction on how to set the tenant
     *
     * @param tenant the new tenant
     */
    public void setTenant(String tenant) {
        if (user != null) {
            user.setTenant(tenant);
        }
        this.tenant = tenant;
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
     * @param roleName the role name
     */
    public void setRole(String roleName) {
        if (user == null) {
            throw new IllegalStateException("role cannot be set while user has not been set!");
        }
        user.setRole(roleName);
    }

    /**
     * @return the user
     */
    public UserDetails getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(UserDetails user) {
        this.user = user;
    }

    public Map<String, Object> getAdditionalParams() {
        return additionalParams;
    }

    public void setAdditionalParams(Map<String, Object> additionalParams) {
        this.additionalParams = additionalParams;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        JWTAuthentication that = (JWTAuthentication) o;

        return jwt != null ? jwt.equals(that.jwt) : that.jwt == null;
    }

    @Override
    public int hashCode() {
        return jwt != null ? jwt.hashCode() : 0;
    }
}
