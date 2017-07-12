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
package fr.cnes.regards.modules.accessrights.service.projectuser;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;

/**
 * Strategy interface to handle Read an Update operations on access settings.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IAccessSettingsService {

    /**
     * Retrieve the {@link AccountSettings}.
     *
     * @return The {@link AccountSettings}
     * @throws EntityNotFoundException
     *             hrown when an {@link AccountSettings} with passed id could not be found
     */
    AccessSettings retrieve() throws EntityNotFoundException;

    /**
     * Update the {@link AccountSettings}.
     *
     * @param pAccessSettings
     *            The {@link AccountSettings}
     * @return The updated access settings
     * @throws EntityNotFoundException
     *             Thrown when an {@link AccountSettings} with passed id could not be found
     */
    AccessSettings update(AccessSettings pAccessSettings) throws EntityNotFoundException;
}
