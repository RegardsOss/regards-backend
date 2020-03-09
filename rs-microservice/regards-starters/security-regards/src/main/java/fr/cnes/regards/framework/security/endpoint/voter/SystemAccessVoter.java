/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import fr.cnes.regards.framework.security.utils.endpoint.ISystemAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;

/**
 * This class authorizes access to all endpoints for system internal call between microservices.
 * @author Sébastien Binda
 * @author Marc Sordi
 */
public class SystemAccessVoter implements ISystemAccessVoter {

    @Override
    public int vote(final Authentication pAuthentication, final Object pObject,
            final Collection<ConfigAttribute> pAttributes) {
        // Default behavior : deny access
        int access = ACCESS_DENIED;

        // If authentication do not contains authority, deny access
        if ((pAuthentication.getAuthorities() != null) && !pAuthentication.getAuthorities().isEmpty()) {
            for (final GrantedAuthority auth : pAuthentication.getAuthorities()) {
                if (RoleAuthority.isSysRole(auth.getAuthority())) {
                    access = ACCESS_GRANTED;
                    break;
                }
            }
        }

        return access;
    }

    @Override
    public boolean supports(final ConfigAttribute pArg0) {
        return true;
    }

    @Override
    public boolean supports(final Class<?> pArg0) {
        return true;
    }

}
