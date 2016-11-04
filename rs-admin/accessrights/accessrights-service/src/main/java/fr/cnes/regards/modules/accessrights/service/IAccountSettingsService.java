/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;

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
     */
    AccountSettings update(AccountSettings pSettings);
}
