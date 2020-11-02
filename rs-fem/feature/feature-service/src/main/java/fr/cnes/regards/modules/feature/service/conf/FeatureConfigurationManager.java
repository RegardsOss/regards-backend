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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.dump.domain.DumpSettings;
import fr.cnes.regards.framework.modules.dump.service.settings.IDumpSettingsService;
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
    private FeatureSaveMetadataScheduler featureSaveMetadataScheduler;

    @Autowired
    private IFeatureNotificationSettingsService notificationSettingsService;

    @Autowired
    private IDumpSettingsService dumpSettingsService;

    @Override
    protected Set<String> importConfiguration(ModuleConfiguration configuration) {
        Set<String> importErrors = new HashSet<>();
        for (ModuleConfigurationItem<?> item : configuration.getConfiguration()) {
            if (DumpSettings.class.isAssignableFrom(item.getKey())) {
                try {
                    featureSaveMetadataScheduler.updateDumpAndScheduler(item.getTypedValue());
                } catch (ModuleException e) {
                    importErrors.add(String.format("New dump settings were not updated, cause by: %s", e.getMessage()));
                    LOGGER.error("Not able to update new dump settings, cause by:", e);
                }
            } else if (FeatureNotificationSettings.class.isAssignableFrom(item.getKey())) {
                try {
                    notificationSettingsService.update(item.getTypedValue());
                } catch (EntityNotFoundException e) {
                    importErrors.add(String.format("New notification settings were not updated, cause by: %s",
                                                   e.getMessage()));
                    LOGGER.error("Not able to update new notification settings, cause by:", e);
                }
            } else {
                String message = String.format(
                        "Configuration item of type %s has been ignored while import because it cannot be handled by %s. Module %s",
                        item.getKey(),
                        this.getClass().getName(),
                        configuration.getModule().getName());
                importErrors.add(message);
                LOGGER.warn(message);
            }
        }
        return importErrors;
    }

    @Override
    public ModuleConfiguration exportConfiguration() {
        List<ModuleConfigurationItem<?>> configurations = new ArrayList<>();

        DumpSettings dumpSettings = dumpSettingsService.retrieve();
        if (dumpSettings != null) {
            configurations.add(ModuleConfigurationItem.build(dumpSettings));
        }

        FeatureNotificationSettings notifSettings = notificationSettingsService.retrieve();
        if (notifSettings != null) {
            configurations.add(ModuleConfigurationItem.build(notifSettings));
        }

        return ModuleConfiguration.build(info, true, configurations);
    }

    @Override
    public Set<String> resetConfiguration() {
        Set<String> errors = new HashSet<>();
        try {
            dumpSettingsService.resetSettings();
        } catch (Exception e) {
            String error = "Error occurred while resetting dump settings.";
            LOGGER.error(error, e);
            errors.add(error);
        }
        try {
            notificationSettingsService.resetSettings();
        } catch (Exception e) {
            String error = "Error occurred while resetting notification settings.";
            LOGGER.error(error, e);
            errors.add(error);
        }
        return errors;
    }
}
