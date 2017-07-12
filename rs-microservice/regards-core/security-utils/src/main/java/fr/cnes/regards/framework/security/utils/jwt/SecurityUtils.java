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
package fr.cnes.regards.framework.security.utils.jwt;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class to extract information from the {@link JWTAuthentication} token.
 *
 * @author Marc Sordi
 * @author Christophe Mertz
 *
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * @return role hold by security context
     */
    public static String getActualRole() {
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if ((authentication != null) && (authentication.getUser() != null)) {
            return authentication.getUser().getRole();
        } else {
            return null;
        }
    }

    /**
     * If any security context found, inject one with specified role.</br>
     * This method must not be used in production and is supplied for test purpose only.
     *
     * @param role
     *            role to mock
     */
    // FIXME trouver un autre moyen de mocker le role / Voir powermock
    public static void mockActualRole(String role) {
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            authentication = new JWTAuthentication(null);
            UserDetails details = new UserDetails();
            details.setRole(role);
            authentication.setUser(details);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            authentication.getUser().setRole(role);
        }
    }

    /**
     * Get the user name from the {@link JWTAuthentication}.<</br>
     * If any security context found, return null.  
     * 
     * @return the user name
     */
    public static String getActualUser() {
        JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext().getAuthentication();
        if ((authentication != null) && (authentication.getUser() != null)) {
            return authentication.getUser().getName();
        } else {
            return null;
        }
    }
}
