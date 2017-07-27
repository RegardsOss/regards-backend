/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.access.services.service.ui;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.access.services.dao.ui.ILinkUIPluginsDatasetsRepository;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginConfigurationRepository;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginDefinitionRepository;
import fr.cnes.regards.modules.access.services.domain.ui.LinkUIPluginsDatasets;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginTypesEnum;

/**
 *
 * Class PluginConfigurationService
 *
 * Business service to manage {@link UIPluginConfiguration} entities.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Service(value = "pluginConfigurationService")
@RegardsTransactional
public class UIPluginConfigurationService implements IUIPluginConfigurationService {

    @Autowired
    private IUIPluginDefinitionRepository pluginRepository;

    @Autowired
    private ILinkUIPluginsDatasetsRepository linkedUiPluginRespository;

    @Autowired
    private IUIPluginConfigurationRepository repository;

    @Override
    public Page<UIPluginConfiguration> retrievePluginConfigurations(final UIPluginTypesEnum pPluginType,
            final Boolean pIsActive, final Boolean pIsLinkedToAllEntities, final Pageable pPageable) {
        if ((pPluginType == null) && (pIsActive == null) && (pIsLinkedToAllEntities == null)) {
            return repository.findAll(pPageable);
        } else {
            if (pPluginType != null) {
                if ((pIsActive != null) && (pIsLinkedToAllEntities != null)) {
                    return repository.findByPluginDefinitionTypeAndActiveAndLinkedToAllEntities(pPluginType, pIsActive,
                                                                                                pIsLinkedToAllEntities,
                                                                                                pPageable);
                } else if (pIsActive != null) {
                    return repository.findByPluginDefinitionTypeAndActive(pPluginType, pIsActive, pPageable);
                } else {
                    return repository.findByPluginDefinitionType(pPluginType, pPageable);
                }
            } else if ((pIsActive != null) && (pIsLinkedToAllEntities != null)) {
                return repository.findByActiveAndLinkedToAllEntities(pIsActive, pIsLinkedToAllEntities, pPageable);
            } else if (pIsLinkedToAllEntities != null) {
                return repository.findByLinkedToAllEntities(pIsLinkedToAllEntities, pPageable);
            } else {
                return repository.findByActive(pIsActive, pPageable);
            }
        }
    }

    @Override
    public Page<UIPluginConfiguration> retrievePluginConfigurations(final UIPluginDefinition pPluginDefinition,
            final Boolean pIsActive, final Boolean pIsLinkedToAllEntities, final Pageable pPageable)
            throws EntityException {
        if ((pPluginDefinition == null) || (pPluginDefinition.getId() == null)) {
            throw new EntityInvalidException("Plugin Identifier cannot be null");
        }

        // retrieve plugin
        if (!pluginRepository.exists(pPluginDefinition.getId())) {
            throw new EntityNotFoundException(pPluginDefinition.getId(), UIPluginDefinition.class);
        }
        return repository.findByPluginDefinition(pPluginDefinition, pPageable);
    }

    @Override
    public UIPluginConfiguration retrievePluginconfiguration(final Long pPluginConfigurationId)
            throws EntityInvalidException {
        if (pPluginConfigurationId == null) {
            throw new EntityInvalidException("Plugin Identifier cannot be null");
        }

        return repository.findOne(pPluginConfigurationId);
    }

    @Override
    public UIPluginConfiguration updatePluginconfiguration(final UIPluginConfiguration pPluginConfiguration)
            throws EntityException {
        if ((pPluginConfiguration == null) || (pPluginConfiguration.getId() == null)) {
            throw new EntityInvalidException("PluginConfiguration Identifier cannot be null");
        }

        if (!repository.exists(pPluginConfiguration.getId())) {
            throw new EntityNotFoundException(pPluginConfiguration.getId(), UIPluginConfiguration.class);
        }

        // Check configuration json format
        final Gson gson = new Gson();
        try {
            gson.fromJson(pPluginConfiguration.getConf(), Object.class);
        } catch (final Exception e) {
            throw new EntityInvalidException("Configuration is not a valid json format.");
        }

        return repository.save(pPluginConfiguration);
    }

    @Override
    public UIPluginConfiguration createPluginconfiguration(final UIPluginConfiguration pPluginConfiguration)
            throws EntityException {
        if ((pPluginConfiguration == null) || (pPluginConfiguration.getId() != null)) {
            throw new EntityInvalidException("PluginConfiguration is invalid");
        }

        // Check configuration json format
        final Gson gson = new Gson();
        try {
            gson.fromJson(pPluginConfiguration.getConf(), Object.class);
        } catch (final Exception e) {
            throw new EntityInvalidException("Configuration is not a valid json format.");
        }

        return repository.save(pPluginConfiguration);
    }

    @Override
    public void deletePluginconfiguration(final UIPluginConfiguration pPluginConfiguration) throws EntityException {
        if ((pPluginConfiguration == null) || (pPluginConfiguration.getId() == null)) {
            throw new EntityInvalidException("PluginConfiguration Identifier cannot be null");
        }

        if (!repository.exists(pPluginConfiguration.getId())) {
            throw new EntityNotFoundException(pPluginConfiguration.getId(), UIPluginConfiguration.class);
        }
        repository.delete(pPluginConfiguration);

    }

    @Override
    public List<UIPluginConfiguration> retrieveActivePluginServices(final String pDatasetId) {
        final List<UIPluginConfiguration> activePluginsConfigurations = new ArrayList<>();
        final LinkUIPluginsDatasets link = linkedUiPluginRespository.findOneByDatasetId(pDatasetId);
        if (link != null) {
            link.getServices().forEach(pluginConf -> {
                if (pluginConf.getActive()) {
                    activePluginsConfigurations.add(pluginConf);
                }
            });
        }

        // Retrieve plugins linked to all dataset
        final List<UIPluginConfiguration> linkedToAllDataset = repository
                .findByActiveAndLinkedToAllEntitiesAndPluginDefinitionType(true, true, UIPluginTypesEnum.SERVICE);
        if (linkedToAllDataset != null) {
            linkedToAllDataset.forEach(pluginConf -> {
                if (!activePluginsConfigurations.contains(pluginConf)) {
                    activePluginsConfigurations.add(pluginConf);
                }
            });
        }
        return activePluginsConfigurations;
    }

}
