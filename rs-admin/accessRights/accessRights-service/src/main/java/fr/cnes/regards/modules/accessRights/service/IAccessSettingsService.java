/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import fr.cnes.regards.modules.accessRights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;

/**
 * Strategy interface to handle Read an Update operations on access settings.
 *
 * @author CS SI
 */
public interface IAccessSettingsService {

    /**
     * Retrieve the {@link AccessSettings}.
     *
     * @return The {@link AccessSettings}
     */
    AccessSettings retrieve();

    /**
     * Update the {@link AccessSettings}.
     *
     * @param pAccessSettings
     *            The {@link AccessSettings}
     * @return The updated access settings
     * @throws EntityNotFoundException
     *             Thrown when an {@link AccessSettings} with passed id could not be found
     */
    AccessSettings update(AccessSettings pAccessSettings) throws EntityNotFoundException;
}
