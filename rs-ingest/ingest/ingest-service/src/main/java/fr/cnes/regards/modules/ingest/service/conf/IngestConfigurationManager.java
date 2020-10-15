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
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.settings.DumpSettings;
import fr.cnes.regards.modules.ingest.domain.settings.AIPNotificationSettings;
import fr.cnes.regards.modules.ingest.service.chain.IIngestProcessingChainService;
import fr.cnes.regards.modules.ingest.service.settings.IDumpManagerService;
import fr.cnes.regards.modules.ingest.service.settings.IAIPNotificationSettingsService;

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
    private IDumpManagerService dumpManagerService;

    @Autowired
    private IAIPNotificationSettingsService notificationSettingsService;


    @Override
    public Set<String> importConfiguration(ModuleConfiguration configuration) {
        Set<String> importErrors = new HashSet<>();
        for (ModuleConfigurationItem<?> item : configuration.getConfiguration()) {
            if (IngestProcessingChain.class.isAssignableFrom(item.getKey())) {
                IngestProcessingChain ipc = item.getTypedValue();
                if (processingService.existsChain(ipc.getName())) {
                    importErrors.add(String
                            .format("Ingest processing chain already exists with same name, skipping import of %s.",
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
                    dumpManagerService.updateDumpAndScheduler(item.getTypedValue());
                } catch (ModuleException e) {
                    LOGGER.error("Not able to update new dump settings, cause by:", e.getMessage());
                }
            } else if (AIPNotificationSettings.class.isAssignableFrom(item.getKey())) {
                try {
                    notificationSettingsService.update(item.getTypedValue());
                } catch (EntityNotFoundException e) {
                    LOGGER.error("Not able to update new notification settings, cause by:", e.getMessage());
                }
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
        DumpSettings dumpSettings = dumpManagerService.getCurrentDumpSettings();
        if(dumpSettings != null) {
            configuration.add(ModuleConfigurationItem.build(dumpSettings));
        }

        AIPNotificationSettings notifSettings = notificationSettingsService.getCurrentNotificationSettings();
        if(notifSettings != null) {
            configuration.add(ModuleConfigurationItem.build(notifSettings));
        }

        return ModuleConfiguration.build(info, configuration);
    }
}
