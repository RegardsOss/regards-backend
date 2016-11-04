/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.List;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessrights.dao.instance.IAccountSettingsRepository;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;

/**
 * {@link IAccountSettingsService} implementation
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
public class AccountSettingsService implements IAccountSettingsService {

    /**
     * CRUD repository managing access settings. Autowired by Spring.
     */
    private final IAccountSettingsRepository accountSettingsRepository;

    /**
     * Creates an {@link AccountSettingsService} wired to the given {@link IAccountSettingsRepository}.
     *
     * @param pAccountSettingsRepository
     *            Autowired by Spring. Must not be {@literal null}.
     */
    public AccountSettingsService(final IAccountSettingsRepository pAccountSettingsRepository) {
        super();
        accountSettingsRepository = pAccountSettingsRepository;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.IAccountSettingsService#retrieve()
     */
    @Override
    public AccountSettings retrieve() {
        final List<AccountSettings> settings = accountSettingsRepository.findAll();
        AccountSettings result = null;
        if (!settings.isEmpty()) {
            result = settings.get(0);
        } else {
            result = new AccountSettings();
            result.setId(0L);
            accountSettingsRepository.save(result);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.IAccountSettingsService#update()
     */
    @Override
    public AccountSettings update(final AccountSettings pAccessSettings) {
        pAccessSettings.setId(0L);
        return accountSettingsRepository.save(pAccessSettings);
    }

}
