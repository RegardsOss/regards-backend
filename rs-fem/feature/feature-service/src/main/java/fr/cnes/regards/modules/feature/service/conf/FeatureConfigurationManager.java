/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.service.conf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import fr.cnes.regards.framework.modules.dump.domain.DumpSettings;
import fr.cnes.regards.framework.modules.dump.service.IDumpSettingsService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.feature.domain.settings.FeatureNotificationSettings;
import fr.cnes.regards.modules.feature.service.settings.IFeatureNotificationSettingsService;
import fr.cnes.regards.modules.feature.service.task.FeatureSaveMetadataScheduler;

/**
 * Configuration manager for current module
 * @author Marc Sordi
 */
@Component
public class FeatureConfigurationManager extends AbstractModuleManager<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureConfigurationManager.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private FeatureSaveMetadataScheduler featureSaveMetadataScheduler;

    @Autowired
    private IFeatureNotificationSettingsService notificationSettingsService;

    @Autowired
    private IDumpSettingsService dumpSettingsService;

    @Override
    public Set<String> resetConfiguration() {
        Set<String> errors = Sets.newHashSet();
        for (PluginConfiguration p : pluginService.getAllPluginConfigurations()) {
            try {
                pluginService.deletePluginConfiguration(p.getBusinessId());
            } catch (ModuleException e) {
                LOGGER.warn(RESET_FAIL_MESSAGE, e);
                errors.add(e.getMessage());
            }
        }
        return errors;
    }

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration configuration) {
        Set<String> importErrors = new HashSet<>();
        for (ModuleConfigurationItem<?> item : configuration.getConfiguration()) {
            if (PluginConfiguration.class.isAssignableFrom(item.getKey())) {
                PluginConfiguration plgConf = item.getTypedValue();
                try {
                    Optional<PluginConfiguration> existingOne = loadPluginConfiguration(plgConf.getBusinessId());
                    if (existingOne.isPresent()) {
                        existingOne.get().setLabel(plgConf.getLabel());
                        existingOne.get().setParameters(plgConf.getParameters());
                        pluginService.updatePluginConfiguration(existingOne.get());
                    } else {
                        pluginService.savePluginConfiguration(plgConf);
                    }
                } catch (ModuleException e) {
                    LOGGER.warn(IMPORT_FAIL_MESSAGE, e);
                    importErrors.add(e.getMessage());
                }
            } else if (DumpSettings.class.isAssignableFrom(item.getKey())) {
                try {
                    featureSaveMetadataScheduler.updateDumpAndScheduler(item.getTypedValue());
                } catch (ModuleException e) {
                    LOGGER.error("Not able to update new dump settings, cause by:", e);
                }
            } else if (FeatureNotificationSettings.class.isAssignableFrom(item.getKey())) {
                try {
                    notificationSettingsService.update(item.getTypedValue());
                } catch (EntityNotFoundException e) {
                    LOGGER.error("Not able to update new notification settings, cause by:", e);
                }
            }
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
    public ModuleConfiguration exportConfiguration() {
        List<ModuleConfigurationItem<?>> configurations = new ArrayList<>();
        // export connections
        for (PluginConfiguration factory : pluginService.getAllPluginConfigurations()) {
            // All connection should be active
            PluginConfiguration exportedConf = pluginService.prepareForExport(factory);
            exportedConf.setIsActive(true);
            configurations.add(ModuleConfigurationItem.build(exportedConf));
        }

        DumpSettings dumpSettings = dumpSettingsService.retrieve();
        if (dumpSettings != null) {
            configurations.add(ModuleConfigurationItem.build(dumpSettings));
        }

        FeatureNotificationSettings notifSettings = notificationSettingsService.getCurrentNotificationSettings();
        if (notifSettings != null) {
            configurations.add(ModuleConfigurationItem.build(notifSettings));
        }

        return ModuleConfiguration.build(info, true, configurations);
    }
}
