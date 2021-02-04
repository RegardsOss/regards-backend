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

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.dao.projects.IAccessSettingsRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettingsEvent;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.springframework.stereotype.Service;

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

    private final IRoleRepository roleRepository;

    private final IPublisher publisher;

    /**
     * Creates an {@link AccessSettingsService} wired to the given {@link IProjectUserRepository}.
     * @param pAccessSettingsRepository Autowired by Spring. Must not be {@literal null}.
     */
    public AccessSettingsService(IAccessSettingsRepository pAccessSettingsRepository, IRoleRepository pRoleRepository, IPublisher pPublisher) {
        super();
        accessSettingsRepository = pAccessSettingsRepository;
        roleRepository = pRoleRepository;
        publisher = pPublisher;
    }

    @Override
    public AccessSettings retrieve() {
        return List.ofAll(accessSettingsRepository.findAll())
            .headOption()
            .orElse(() -> {
                AccessSettings alt = new AccessSettings();
                alt.setId(0L);
                return Option.some(alt);
            })
            .map(result -> {
                if (result.getDefaultRole() != null && result.getDefaultGroups() != null) {
                    return result;
                } else {
                    if (result.getDefaultRole() == null) {
                        roleRepository
                            .findOneByName(DefaultRole.REGISTERED_USER.toString())
                            .ifPresent(result::setDefaultRole);
                    }
                    if (result.getDefaultGroups() == null) {
                        result.setDefaultGroups(Lists.newArrayList());
                    }
                    return accessSettingsRepository.save(result);
                }
            })
            .get();
    }

    @Override
    public AccessSettings update(AccessSettings accessSettings) throws EntityNotFoundException {
        if (!accessSettingsRepository.existsById(accessSettings.getId())) {
            throw new EntityNotFoundException(accessSettings.getId().toString(), AccessSettings.class);
        }
        if (accessSettings.getDefaultRole() == null) {
            roleRepository
                .findOneByName(DefaultRole.REGISTERED_USER.toString())
                .ifPresent(accessSettings::setDefaultRole);
        }
        if (accessSettings.getDefaultGroups() == null) {
            accessSettings.setDefaultGroups(Lists.newArrayList());
        }
        AccessSettings result = accessSettingsRepository.save(accessSettings);
        publisher.publish(new AccessSettingsEvent(
            accessSettings.getMode(),
            accessSettings.getDefaultRole().getName(),
            accessSettings.getDefaultGroups()
        ));
        return result;
    }

}
