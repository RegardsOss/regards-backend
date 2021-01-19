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

import java.util.List;

import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.instance.dao.IAccountSettingsRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSettings;

/**
 * {@link IAccountSettingsService} implementation
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
@InstanceTransactional
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
     * @see fr.cnes.regards.modules.accessrights.service.role.IAccountSettingsService#retrieve()
     */
    @Override
    public AccountSettings retrieve() {
        final List<AccountSettings> settings = accountSettingsRepository.findAll();
        AccountSettings result;
        if (!settings.isEmpty()) {
            result = settings.get(0);
        } else {
            result = new AccountSettings();
            result = accountSettingsRepository.save(result);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.role.IAccountSettingsService#update()
     */
    @Override
    public AccountSettings update(final AccountSettings accountSettings) throws EntityNotFoundException {
        if (!accountSettingsRepository.existsById(accountSettings.getId())) {
            throw new EntityNotFoundException(accountSettings.getId().toString(), AccountSettings.class);
        }
        return accountSettingsRepository.save(accountSettings);
    }

}
