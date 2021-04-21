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
package fr.cnes.regards.modules.accessrights.instance.service;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSettings;

/**
 * Strategy interface to handle Read an Update operations on account settings.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IAccountSettingsService {

    /**
     * Retrieve the {@link AccountSettings}.
     *
     * @return The {@link AccountSettings}
     */
    AccountSettings retrieve();

    /**
     * Update the {@link AccountSettings}.
     *
     * @param pSettings
     *            The {@link AccountSettings}
     * @return The updated account settings
     * @throws EntityNotFoundException
     */
    AccountSettings update(AccountSettings pSettings) throws EntityNotFoundException;
}
