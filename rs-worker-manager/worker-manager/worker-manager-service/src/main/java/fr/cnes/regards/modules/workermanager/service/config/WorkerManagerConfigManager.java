/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.workermanager.service.config;

import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractModuleManagerWithTenantSettings;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerConfig;
import fr.cnes.regards.modules.workermanager.domain.config.Workflow;
import fr.cnes.regards.modules.workermanager.dto.WorkerConfigDto;
import fr.cnes.regards.modules.workermanager.dto.WorkflowDto;
import fr.cnes.regards.modules.workermanager.service.cache.confupdated.WorkerConfUpdatedEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Module to allow import/export worker manager microservice configuration
 * See {@link WorkerConfigService}
 *
 * @author Léo Mieulet
 */
@Component
public class WorkerManagerConfigManager extends AbstractModuleManagerWithTenantSettings<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerManagerConfigManager.class);

    private final WorkerConfigService workerConfigService;

    private final WorkerConfUpdatedEventPublisher workerConfUpdatedEventPublisher;

    private final WorkflowConfigService workflowConfigService;

    public WorkerManagerConfigManager(WorkerConfigService workerConfigService,
                                      WorkerConfUpdatedEventPublisher workerConfUpdatedEventPublisher,
                                      WorkflowConfigService workflowConfigService) {
        this.workerConfigService = workerConfigService;
        this.workerConfUpdatedEventPublisher = workerConfUpdatedEventPublisher;
        this.workflowConfigService = workflowConfigService;
    }

    @Override
    public ModuleConfiguration exportConfiguration(List<ModuleConfigurationItem<?>> configuration) {
        for (WorkerConfig conf : workerConfigService.searchAll()) {
            configuration.add(ModuleConfigurationItem.build(conf.toDto()));
        }
        for (Workflow workflow : workflowConfigService.findAll()) {
            configuration.add(ModuleConfigurationItem.build(workflow.toDto()));
        }
        return ModuleConfiguration.build(info, configuration);
    }

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration configuration, Set<String> importErrors) {
        List<ModuleConfigurationItem<?>> configurationItems = configuration.getConfiguration();
        Set<WorkerConfigDto> workerConfigs = getWorkerConfigs(configurationItems);
        Set<WorkflowDto> workflows = getWorkflow(configurationItems);

        // Import configuration into transaction
        importErrors.addAll(workerConfigService.importConfiguration(workerConfigs));
        importErrors.addAll(workflowConfigService.importConfiguration(workflows));

        // Clear cache
        workerConfUpdatedEventPublisher.publishEvent();

        return importErrors;
    }

    @Override
    public Set<String> resetConfiguration(Set<String> errors) {
        // delete all worker configurations
        for (WorkerConfig conf : workerConfigService.searchAll()) {
            workerConfigService.delete(conf);
        }
        // delete all workflow configurations
        workflowConfigService.deleteAllInBatch();
        // publish conf updated event
        workerConfUpdatedEventPublisher.publishEvent();
        return errors;
    }

    private Set<WorkerConfigDto> getWorkerConfigs(Collection<ModuleConfigurationItem<?>> items) {
        return items.stream()
                    .filter(i -> WorkerConfigDto.class.isAssignableFrom(i.getKey()))
                    .map(i -> (WorkerConfigDto) i.getTypedValue())
                    .collect(Collectors.toSet());
    }

    private Set<WorkflowDto> getWorkflow(Collection<ModuleConfigurationItem<?>> items) {
        return items.stream()
                    .filter(i -> WorkflowDto.class.isAssignableFrom(i.getKey()))
                    .map(i -> (WorkflowDto) i.getTypedValue())
                    .collect(Collectors.toSet());
    }
}
