/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser;

import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;

/**
 * Strategy interface to handle Read an Update operations on access settings.
 *
 * @author CS SI
 */
public interface IAccessSettingsService {

    /**
     * Retrieve the {@link AccountSettings}.
     *
     * @return The {@link AccountSettings}
     */
    AccessSettings retrieve();

    /**
     * Update the {@link AccountSettings}.
     *
     * @param pAccessSettings
     *            The {@link AccountSettings}
     * @return The updated access settings
     * @throws ModuleEntityNotFoundException
     *             Thrown when an {@link AccountSettings} with passed id could not be found
     */
    AccessSettings update(AccessSettings pAccessSettings) throws ModuleEntityNotFoundException;
}
