/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.authentication.plugins;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.modules.authentication.plugins.domain.ExternalAuthenticationInformations;

/**
 * Class IServiceProviderPlugin
 *
 * Interface for all Service Provider plugins.
 * @author Sébastien Binda
 */
@PluginInterface(description = "Interface for all Service Provider.")
public interface IServiceProviderPlugin {

    /**
     * Verify authentication ticket with the current service provider
     * @param pAuthInformations Authentication informations
     * @return [true|false]
     */
    boolean checkTicketValidity(ExternalAuthenticationInformations pAuthInformations);

    /**
     * Retrieve user informations with the current service provider
     * @param pAuthInformations Authentication informations
     * @return {@link UserDetails}
     */
    UserDetails getUserInformations(ExternalAuthenticationInformations pAuthInformations);

}
