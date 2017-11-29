/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.service.role;

/**
 * POJO holding a role and a jwt generated for this role
 * @author Sylvain Vissiere-Guerinet
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

    /**
     * Constructor setting the parameters as attributes
     * @param pJwt
     * @param pRoleName
     */
    public CoupleJwtRole(String pJwt, String pRoleName) {
        access_token = pJwt;
        role = pRoleName;
    }

    /**
     * @return the role
     */
    public String getRole() {
        return role;
    }

    /**
     * Set the role
     * @param pRole
     */
    public void setRole(String pRole) {
        role = pRole;
    }

    /**
     * @return the access token
     */
    public String getAccessToken() {
        return access_token;
    }

    /**
     * Set the access token
     * @param pAccessToken
     */
    public void setAccessToken(String pAccessToken) {
        access_token = pAccessToken;
    }

}
