/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessrights.dao.instance.IAccountSettingsRepository;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;

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
        return accountSettingsRepository.findAll().get(0);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.IAccountSettingsService#update()
     */
    @Override
    public AccountSettings update(final AccountSettings pAccessSettings) throws EntityNotFoundException {
        if (!accountSettingsRepository.exists(pAccessSettings.getId())) {
            throw new EntityNotFoundException(pAccessSettings.getId().toString(), AccountSettings.class);
        }
        return accountSettingsRepository.save(pAccessSettings);
    }

}
