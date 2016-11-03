/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;

/**
 * Strategy interface to handle Read an Update operations on account settings.
 *
 * @author CS SI
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
     *
     * @throws EntityNotFoundException
     *             Thrown when an {@link AccountSettings} with passed id could not be found
     */
    AccountSettings update(AccountSettings pSettings) throws EntityNotFoundException;
}
