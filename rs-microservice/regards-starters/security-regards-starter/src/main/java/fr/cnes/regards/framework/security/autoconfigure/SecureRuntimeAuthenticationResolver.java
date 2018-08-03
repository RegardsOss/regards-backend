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
package fr.cnes.regards.framework.security.autoconfigure;

import org.springframework.security.core.context.SecurityContextHolder;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 * Retrieve user and role according to security context
 *
 * @author Marc Sordi
 *
 */
public class SecureRuntimeAuthenticationResolver implements IAuthenticationResolver {

    @Override
    public String getUser() {
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if ((authentication != null) && (authentication.getUser() != null)) {
            return authentication.getUser().getName();
        } else {
            return null;
        }
    }

    @Override
    public String getRole() {
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if ((authentication != null) && (authentication.getUser() != null)) {
            return authentication.getUser().getRole();
        } else {
            return null;
        }
    }

    @Override
    public String getToken() {
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getJwt();
        } else {
            return null;
        }
    }
}
