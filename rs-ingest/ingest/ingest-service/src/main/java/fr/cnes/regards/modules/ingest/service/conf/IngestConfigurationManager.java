/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.conf;

import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.dump.domain.DumpSettings;
import fr.cnes.regards.framework.modules.dump.service.settings.IDumpSettingsService;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.settings.AIPNotificationSettings;
import fr.cnes.regards.modules.ingest.service.chain.IIngestProcessingChainService;
import fr.cnes.regards.modules.ingest.service.schedule.AIPSaveMetadataScheduler;
import fr.cnes.regards.modules.ingest.service.settings.IAIPNotificationSettingsService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Configuration manager for current module
 * @author Marc Sordi
 */
@Component
public class IngestConfigurationManager extends AbstractModuleManager<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestConfigurationManager.class);

    @Autowired
    private IIngestProcessingChainService processingService;

    @Autowired
    private IIngestProcessingChainRepository ingestChainRepository;

    @Autowired
    private AIPSaveMetadataScheduler aipSaveMetadataScheduler;

    @Autowired
    private IDumpSettingsService dumpSettingsService;

    @Autowired
    private IAIPNotificationSettingsService notificationSettingsService;

    @Override
    public Set<String> importConfiguration(ModuleConfiguration configuration) {
        Set<String> importErrors = new HashSet<>();
        for (ModuleConfigurationItem<?> item : configuration.getConfiguration()) {
            if (IngestProcessingChain.class.isAssignableFrom(item.getKey())) {
                IngestProcessingChain ipc = item.getTypedValue();
                if (processingService.existsChain(ipc.getName())) {
                    importErrors.add(String.format(
                            "Ingest processing chain already exists with same name, skipping import of %s.",
                            ipc.getName()));
                } else {
                    try {
                        processingService.createNewChain(ipc);
                    } catch (ModuleException e) {
                        importErrors.add(String.format("Skipping import of IngestProcessingChain %s: %s", ipc.getName(),
                                                       e.getMessage()));
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            } else if (DumpSettings.class.isAssignableFrom(item.getKey())) {
                try {
                    aipSaveMetadataScheduler.updateDumpAndScheduler(item.getTypedValue());
                } catch (ModuleException e) {
                    importErrors.add(String.format("New dump settings were not updated, cause by: %s", e.getMessage()));
                    LOGGER.error("New dump settings were not updated, cause by:", e);
                }
            } else if (AIPNotificationSettings.class.isAssignableFrom(item.getKey())) {
                notificationSettingsService.update(item.getTypedValue());
            }
        }
        return importErrors;
    }

    @Override
    public ModuleConfiguration exportConfiguration() {
        List<ModuleConfigurationItem<?>> configuration = new ArrayList<>();
        for (IngestProcessingChain ipc : ingestChainRepository.findAll()) {
            configuration.add(ModuleConfigurationItem.build(ipc));
        }
        DumpSettings dumpSettings = dumpSettingsService.retrieve();
        if (dumpSettings != null) {
            configuration.add(ModuleConfigurationItem.build(dumpSettings));
        }

        AIPNotificationSettings notifSettings = notificationSettingsService.retrieve();
        if (notifSettings != null) {
            configuration.add(ModuleConfigurationItem.build(notifSettings));
        }

        return ModuleConfiguration.build(info, configuration);
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
