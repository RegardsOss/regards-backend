/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.security.endpoint.voter;

import java.util.Collection;
import java.util.List;

import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;

import fr.cnes.regards.framework.security.utils.endpoint.IProjectAdminAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 *
 * MethodAuthorization voter to accept access to all endpoints for project administrator.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class ProjectAdminAccessVoter implements IProjectAdminAccessVoter {

    @Override
    public boolean supports(ConfigAttribute pAttribute) {
        return true;
    }

    @Override
    public boolean supports(Class<?> pClazz) {
        return true;
    }

    @Override
    public int vote(Authentication pAuthentication, Object pObject, Collection<ConfigAttribute> pAttributes) {

        final JWTAuthentication authentication = (JWTAuthentication) pAuthentication;

        // If authenticated user is one of the project admins allow all.
        @SuppressWarnings("unchecked")
        final List<RoleAuthority> roles = (List<RoleAuthority>) authentication.getAuthorities();
        if (RoleAuthority.isProjectAdminRole(roles.get(0).getAuthority())) {
            return ACCESS_GRANTED;
        }

        return ACCESS_DENIED;
    }

}
