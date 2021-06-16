/*
 *
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
 *
 */
package fr.cnes.regards.modules.dam.service;

import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.modules.tenant.settings.service.DynamicTenantSettingService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.dam.dao.entities.IDatasetRepository;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IConnectionPlugin;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IDataSourcePlugin;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.DatasetConfiguration;
import fr.cnes.regards.modules.dam.service.entities.IDatasetService;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.service.IModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DAM configuration manager. Exports model & connection plugin configurations & datasource plugin configurations.
 *
 * @author Sylvain VISSIERE-GUERINET
 * @author Sébastien Binda
 * @author Marc SORDI
 * @since V1.6.0 import/export datasets
 */
@Component
public class DamConfigurationManager extends AbstractModuleManager<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DamConfigurationManager.class);

    private final Validator validator;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IDatasetService datasetService;

    private final IDatasetRepository datasetRepository;

    private final IModelService modelService;

    private final IPluginService pluginService;

    private final DynamicTenantSettingService dynamicTenantSettingService;

    public DamConfigurationManager(Validator validator, IRuntimeTenantResolver runtimeTenantResolver,
            IDatasetService datasetService, IDatasetRepository datasetRepository, IModelService modelService,
            IPluginService pluginService, DynamicTenantSettingService dynamicTenantSettingService) {
        this.validator = validator;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.datasetService = datasetService;
        this.datasetRepository = datasetRepository;
        this.modelService = modelService;
        this.pluginService = pluginService;
        this.dynamicTenantSettingService = dynamicTenantSettingService;
    }

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration configuration) {

        Set<String> importErrors = new HashSet<>();

        // First create connections
        for (PluginConfiguration plgConf : getPluginConfigurations(configuration)) {
            try {
                pluginService.savePluginConfiguration(plgConf);
            } catch (EntityInvalidException | EncryptionException | EntityNotFoundException e) {
                importErrors.add(e.getMessage());
            }
        }

        for (DynamicTenantSetting setting : getSettings(configuration)) {
            try {
                dynamicTenantSettingService.update(setting.getName(), setting.getValue());
            } catch (ModuleException e) {
                importErrors.add(String.format("Configuration item not imported : Invalid Tenant Setting %s", setting));
                LOGGER.error("Configuration item not imported : Invalid Tenant Setting {}", setting);
            }
        }

        // Import datasets link to a previously configured datasource
        importErrors.addAll(importDatasets(configuration.getConfiguration()));

        return importErrors;
    }

    @Override
    public ModuleConfiguration exportConfiguration() {
        List<ModuleConfigurationItem<?>> configurations = new ArrayList<>();

        // export connections
        for (PluginConfiguration connection : pluginService.getPluginConfigurationsByType(IConnectionPlugin.class)) {
            // All connection should be active
            PluginConfiguration exportedConnection = pluginService.prepareForExport(connection);
            exportedConnection.setIsActive(true);
            configurations.add(ModuleConfigurationItem.build(exportedConnection));
        }
        // export datasources
        for (PluginConfiguration dataSource : pluginService.getPluginConfigurationsByType(IDataSourcePlugin.class)) {
            configurations.add(ModuleConfigurationItem.build(pluginService.prepareForExport(dataSource)));
        }
        // export settings
        dynamicTenantSettingService.readAll()
                .forEach(setting -> configurations.add(ModuleConfigurationItem.build(setting)));

        // export datasets
        configurations.addAll(exportDatasets());

        return ModuleConfiguration.build(info, configurations);
    }

    /**
     * Get all {@link PluginConfiguration}s of the {@link ModuleConfigurationItem}s
     *
     * @param configuration {@link ModuleConfiguration}s
     * @return {@link PluginConfiguration}s
     */
    private Set<PluginConfiguration> getPluginConfigurations(ModuleConfiguration configuration) {
        return configuration.getConfiguration().stream()
                .filter(i -> PluginConfiguration.class.isAssignableFrom(i.getKey()))
                .map(i -> (PluginConfiguration) i.getTypedValue()).collect(Collectors.toSet());
    }

    private Set<DynamicTenantSetting> getSettings(ModuleConfiguration configuration) {
        return configuration.getConfiguration().stream()
                .filter(i -> DynamicTenantSetting.class.isAssignableFrom(i.getKey()))
                .map(i -> (DynamicTenantSetting) i.getTypedValue()).collect(Collectors.toSet());
    }

    private Set<String> importDatasets(List<ModuleConfigurationItem<?>> items) {
        Set<String> errors = new HashSet<>();
        List<DatasetConfiguration> confs = items.stream()
                .filter(i -> DatasetConfiguration.class.isAssignableFrom(i.getKey()))
                .map(i -> (DatasetConfiguration) i.getTypedValue()).collect(Collectors.toList());

        for (DatasetConfiguration conf : confs) {
            try {
                // Validate conf
                Errors validationErrors = new MapBindingResult(new HashMap<>(), Dataset.class.getName());
                validator.validate(conf, validationErrors);
                if (validationErrors.hasErrors()) {
                    validationErrors.getFieldErrors().forEach(f -> errors.add(f.getDefaultMessage()));
                    continue;
                }
                // Retrieve model
                Model model = modelService.getModelByName(conf.getFeature().getModel());
                // Retrieve datasource
                PluginConfiguration datasource = pluginService.getPluginConfiguration(conf.getDatasource());
                // Validate subsetting clause
                if (!datasetService.validateOpenSearchSubsettingClause(conf.getSubsetting())) {
                    String message = String.format("Cannot import dataset %s cause to an invalid subsetting clause %s",
                                                   conf.getFeature().getId(), conf.getSubsetting());
                    errors.add(message);
                    continue;
                }

                // Create or update if possible (update only available if only one DATASET exists for the specified provider id)
                createOrUpdateDataset(model, datasource, conf, validationErrors);
            } catch (ModuleException mex) {
                LOGGER.error("Dataset import throw an exception", mex);
                String message = String
                        .format("Cannot import dataset %s : %s", conf.getFeature().getId(), mex.getMessage());
                errors.add(message);
            }
        }
        return errors;
    }

    private void createOrUpdateDataset(Model model, PluginConfiguration datasource, DatasetConfiguration conf,
            Errors validationErrors) throws ModuleException {

        // First : try to load dataset from its id or provider id
        Optional<Dataset> existingOne = Optional.empty();
        if (conf.getFeature().getId() != null) {
            Dataset dataset = datasetRepository.findByIpId(conf.getFeature().getId());
            if (dataset == null) {
                String message = String.format("Unknown dataset for id : %s.!", conf.getFeature().getId());
                throw new ModuleException(message);
            } else {
                existingOne = Optional.of(dataset);
            }
        } else {
            Set<Dataset> datasets = datasetService.findAllByProviderId(conf.getFeature().getProviderId());
            if (!datasets.isEmpty()) {
                if (datasets.size() > 1) {
                    String message = String
                            .format("Multiple datasets exist with this provider id : %s. Import cannot select right one! Please fulfil the id to precisely select it!",
                                    conf.getFeature().getProviderId());
                    throw new ModuleException(message);
                } else {
                    existingOne = datasets.stream().findFirst();
                }
            }
        }

        // Create or update dataset
        if (existingOne.isPresent()) {
            // Update dataset
            Dataset dataset = existingOne.get();
            dataset.setProviderId(conf.getFeature().getProviderId());
            dataset.setLabel(conf.getFeature().getLabel());
            dataset.setOpenSearchSubsettingClause(conf.getSubsetting());
            // Override id, virtual id, last, version and model from existing one
            conf.getFeature().setId(dataset.getIpId());
            conf.getFeature().setLast(dataset.isLast()); // Virtual id will be set accordingly
            conf.getFeature().setVersion(dataset.getVersion());
            conf.getFeature().setModel(dataset.getModel().getName()); // model cannot be changed - always override it!
            // Propagate feature
            dataset.setFeature(conf.getFeature());
            // Call service to persist dataset
            datasetService.updateDataset(dataset.getId(), dataset, validationErrors);
        } else {
            // Create new dataset
            Dataset dataset = new Dataset(model, runtimeTenantResolver.getTenant(), conf.getFeature().getProviderId(),
                                          conf.getFeature().getLabel());
            dataset.setDataSource(datasource);
            dataset.setOpenSearchSubsettingClause(conf.getSubsetting());
            dataset.setFeature(conf.getFeature());
            // Call service to persist dataset
            datasetService.createDataset(dataset, validationErrors);
        }
    }

    private List<ModuleConfigurationItem<DatasetConfiguration>> exportDatasets() {
        List<ModuleConfigurationItem<DatasetConfiguration>> exportedDatasets = new ArrayList<>();
        for (Dataset dataset : datasetService.findAll()) {
            DatasetConfiguration configuration = DatasetConfiguration.builder()
                    .datasource(dataset.getDataSource().getBusinessId())
                    .subsetting(dataset.getOpenSearchSubsettingClause()).feature(dataset.getFeature()).build();
            exportedDatasets.add(ModuleConfigurationItem.build(configuration));
        }
        return exportedDatasets;
    }
}
