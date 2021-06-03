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
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IConnectionPlugin;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IDataSourcePlugin;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.DatasetConfiguration;
import fr.cnes.regards.modules.dam.service.entities.IDatasetService;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.service.IModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private Validator validator;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IDatasetService datasetService;

    @Autowired
    private IModelService modelService;

    @Autowired
    private IPluginService pluginService;

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration configuration) {

        Set<String> importErrors = new HashSet<>();

        // First manage plugin configurations
        for (PluginConfiguration plgConf : getPluginConfs(configuration.getConfiguration())) {
            try {
                pluginService.savePluginConfiguration(plgConf);
            } catch (EntityInvalidException | EncryptionException | EntityNotFoundException e) {
                importErrors.add(e.getMessage());
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
        // export datasets
        configurations.addAll(exportDatasets());

        return ModuleConfiguration.build(info, configurations);
    }

    /**
     * Get all {@link PluginConfiguration}s of the {@link ModuleConfigurationItem}s
     *
     * @param items {@link ModuleConfigurationItem}s
     * @return {@link PluginConfiguration}s
     */
    private Set<PluginConfiguration> getPluginConfs(Collection<ModuleConfigurationItem<?>> items) {
        return items.stream().filter(i -> PluginConfiguration.class.isAssignableFrom(i.getKey()))
                .map(i -> (PluginConfiguration) i.getTypedValue()).collect(Collectors.toSet());
    }

    private Set<String> importDatasets(List<ModuleConfigurationItem<?>> items) {
        Set<String> errors = new HashSet<>();
        List<DatasetConfiguration> confs = items
                .stream()
                .filter(i -> DatasetConfiguration.class.isAssignableFrom(i.getKey()))
                .map(i -> (DatasetConfiguration) i.getTypedValue())
                .collect(Collectors.toList());

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
                    String message = String.format("Cannot import dataset %s cause to an invalid subsetting clause %s", conf.getFeature().getId(), conf.getSubsetting());
                    errors.add(message);
                    continue;
                }

                // Create or update if possible (update only available if only one DATASET exists for the specified provider id)
                createOrUpdateDataset(model,datasource,conf,validationErrors);
            } catch (ModuleException mex) {
                LOGGER.error("Dataset import throw an exception", mex);
                String message = String.format("Cannot import dataset %s : %s", conf.getFeature().getId(), mex.getMessage());
                errors.add(message);
            }
        }
        return errors;
    }

    private void createOrUpdateDataset(Model model, PluginConfiguration datasource, DatasetConfiguration  conf, Errors validationErrors) throws ModuleException {
        // First : try to load dataset from its provider id
        Set<Dataset> datasets = datasetService.findAllByProviderId(conf.getFeature().getProviderId());
        if (datasets.isEmpty()) {
            // Create new dataset
            Dataset dataset = new Dataset(model, runtimeTenantResolver.getTenant(), conf.getFeature().getProviderId(), conf.getFeature().getLabel());
            dataset.setDataSource(datasource);
            dataset.setOpenSearchSubsettingClause(conf.getSubsetting());
            dataset.setFeature(conf.getFeature());
            // Call service to persist dataset
            datasetService.createDataset(dataset, validationErrors);
        } else {
            if (datasets.size() > 1) {
                String message = String.format("Multiple datasets exist with this provider id : %s. Import cannot select right one!",conf.getFeature().getProviderId());
                throw new ModuleException(message);
            }
            // Update dataset
            Dataset dataset = datasets.stream().findFirst().get();
            dataset.setProviderId(conf.getFeature().getProviderId());
            dataset.setLabel(conf.getFeature().getLabel());
            dataset.setOpenSearchSubsettingClause(conf.getSubsetting());
            dataset.setFeature(conf.getFeature());
            // Workaround : model cannot be changed - always override it!
            dataset.getFeature().setModel(dataset.getModel().getName());
            // Call service to persist dataset
            datasetService.updateDataset(dataset.getId(),dataset,validationErrors);
        }
    }


    private List<ModuleConfigurationItem<DatasetConfiguration>> exportDatasets() {
        List<ModuleConfigurationItem<DatasetConfiguration>> exportedDatasets = new ArrayList<>();
        for (Dataset dataset : datasetService.findAll()) {
            DatasetConfiguration configuration = DatasetConfiguration.builder()
                    .datasource(dataset.getDataSource().getBusinessId())
                    .subsetting(dataset.getOpenSearchSubsettingClause())
                    .feature(dataset.getFeature()).build();
            exportedDatasets.add(ModuleConfigurationItem.build(configuration));
        }
        return exportedDatasets;
    }
}
