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
package fr.cnes.regards.modules.accessrights.service.projectuser;

import java.util.List;

import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.dao.projects.IAccessSettingsRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;

/**
 * {@link IAccessSettingsService} implementation
 * @author Xavier-Alexandre Brochard
 */
@Service
@MultitenantTransactional
public class AccessSettingsService implements IAccessSettingsService {

    /**
     * CRUD repository managing access settings. Autowired by Spring.
     */
    private final IAccessSettingsRepository accessSettingsRepository;

    /**
     * Creates an {@link AccessSettingsService} wired to the given {@link IProjectUserRepository}.
     * @param pAccessSettingsRepository Autowired by Spring. Must not be {@literal null}.
     */
    public AccessSettingsService(IAccessSettingsRepository pAccessSettingsRepository) {
        super();
        accessSettingsRepository = pAccessSettingsRepository;
    }

    @Override
    public AccessSettings retrieve() {
        List<AccessSettings> list = accessSettingsRepository.findAll();
        AccessSettings result;
        if (list.isEmpty()) {
            result = new AccessSettings();
            result.setId(0L);
            result = accessSettingsRepository.save(result);
        } else {
            result = list.get(0);
        }
        return result;
    }

    @Override
    public AccessSettings update(AccessSettings accessSettings) throws EntityNotFoundException {
        if (!accessSettingsRepository.existsById(accessSettings.getId())) {
            throw new EntityNotFoundException(accessSettings.getId().toString(), AccessSettings.class);
        }
        return accessSettingsRepository.save(accessSettings);
    }

}
