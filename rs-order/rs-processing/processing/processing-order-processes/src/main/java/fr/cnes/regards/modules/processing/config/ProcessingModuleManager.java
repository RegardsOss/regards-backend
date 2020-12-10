/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.service.IProcessPluginConfigService;
import fr.cnes.regards.modules.processing.service.ProcessPluginConfigService.DeleteAttemptOnUsedProcessException;

/**
 * This class is the module manager for the processing module when used in REGARDS in conjunction with rs-order.
 *
 * @author gandrieu
 */
@Component
public class ProcessingModuleManager extends AbstractModuleManager<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingModuleManager.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IProcessPluginConfigService processService;

    @Override
    public Set<String> resetConfiguration() {
        Set<String> errors = Sets.newHashSet();
        for (ProcessPluginConfigurationRightsDTO p : processService.findAllRightsPluginConfigs()) {
            try {
                processService.delete(UUID.fromString(p.getPluginConfiguration().getBusinessId()));
            } catch (DeleteAttemptOnUsedProcessException | ModuleException e) {
                LOGGER.warn(RESET_FAIL_MESSAGE, e);
                errors.add(e.getMessage());
            }
        }
        return errors;
    }

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration configuration) {

        Set<String> importErrors = new HashSet<>();
        Set<PluginConfiguration> configurations = getPluginConfs(configuration.getConfiguration());
        Set<ProcessConfigurationDTO> processes = getProcessConfs(configuration.getConfiguration());

        Set<PluginConfiguration> plugins = Sets.newHashSet();

        // First create connections
        for (PluginConfiguration plgConf : configurations) {
            try {
                Optional<PluginConfiguration> existingOne = loadPluginConfiguration(plgConf.getBusinessId());
                if (existingOne.isPresent()) {
                    existingOne.get().setLabel(plgConf.getLabel());
                    existingOne.get().setParameters(plgConf.getParameters());
                    plugins.add(pluginService.updatePluginConfiguration(existingOne.get()));
                } else {
                    plugins.add(pluginService.savePluginConfiguration(plgConf));
                }
            } catch (ModuleException e) {
                LOGGER.warn(IMPORT_FAIL_MESSAGE, e);
                importErrors.add(e.getMessage());
            }
        }

        for (ProcessConfigurationDTO process : processes) {
            plugins.stream().filter(p -> p.getBusinessId().equals(process.getPluginConfBid())).findFirst()
                    .ifPresent(pc -> {
                        try {
                            processService.create(new ProcessPluginConfigurationRightsDTO(pc, process.getRights()));
                        } catch (EntityNotFoundException e) {
                            LOGGER.error(e.getMessage(), e);
                            importErrors.add(e.getMessage());
                        }
                    });
        }

        return importErrors;

    }

    private Optional<PluginConfiguration> loadPluginConfiguration(String businessId) {
        PluginConfiguration existingOne = null;
        try {
            existingOne = pluginService.getPluginConfiguration(businessId);
        } catch (EntityNotFoundException e) { // NOSONAR
            // Nothing to do, plugin configuration does not exists.
        }
        return Optional.ofNullable(existingOne);
    }

    @Override
    public ModuleConfiguration exportConfiguration() throws ModuleException {
        List<ModuleConfigurationItem<?>> configurations = new ArrayList<>();
        // export connections
        for (PluginConfiguration factory : pluginService.getAllPluginConfigurations()) {
            // All connection should be active
            PluginConfiguration exportedConf = pluginService.prepareForExport(factory);
            exportedConf.setIsActive(true);
            configurations.add(ModuleConfigurationItem.build(exportedConf));
        }
        processService.findAllRightsPluginConfigs().stream().forEach(c -> configurations.add(ModuleConfigurationItem
                .build(new ProcessConfigurationDTO(c.getPluginConfiguration().getBusinessId(), c.getRights()))));
        return ModuleConfiguration.build(info, true, configurations);
    }

    /**
     * Get all {@link PluginConfiguration}s of the {@link ModuleConfigurationItem}s
     * @param items {@link ModuleConfigurationItem}s
     * @return  {@link PluginConfiguration}s
     */
    private Set<PluginConfiguration> getPluginConfs(Collection<ModuleConfigurationItem<?>> items) {
        return items.stream().filter(i -> PluginConfiguration.class.isAssignableFrom(i.getKey()))
                .map(i -> (PluginConfiguration) i.getTypedValue()).collect(Collectors.toSet());
    }

    private Set<ProcessConfigurationDTO> getProcessConfs(Collection<ModuleConfigurationItem<?>> items) {
        return items.stream().filter(i -> ProcessConfigurationDTO.class.isAssignableFrom(i.getKey()))
                .map(i -> (ProcessConfigurationDTO) i.getTypedValue()).collect(Collectors.toSet());
    }
}
