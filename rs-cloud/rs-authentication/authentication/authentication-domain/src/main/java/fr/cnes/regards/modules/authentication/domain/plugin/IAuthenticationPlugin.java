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
package fr.cnes.regards.modules.authentication.domain.plugin;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;

/**
 * Class IAuthenticationProvider
 * <p>
 * Authentication Provider interface.
 *
 * @author Sébastien Binda
 */
@FunctionalInterface
@PluginInterface(description = "Interface for all identity provider plugins.")
public interface IAuthenticationPlugin {

    /**
     * Check if the couple pName/pPassowrd is valid for the given project pScope
     *
     * @param pName     user login
     * @param pPassword user password
     * @param pScope    user project
     * @return Authentication status UserStatus
     */
    AuthenticationPluginResponse authenticate(String pName, String pPassword, String pScope);

}
