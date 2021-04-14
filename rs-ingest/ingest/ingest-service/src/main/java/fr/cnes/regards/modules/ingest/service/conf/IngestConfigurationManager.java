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
package fr.cnes.regards.modules.ingest.service.conf;

import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.dump.service.settings.DumpSettingsService;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.service.chain.IIngestProcessingChainService;
import fr.cnes.regards.modules.ingest.service.settings.AIPNotificationSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration manager for current module
 * @author Marc Sordi
 */
@Component
public class IngestConfigurationManager extends AbstractModuleManager<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestConfigurationManager.class);

    private final IIngestProcessingChainService processingService;
    private final IIngestProcessingChainRepository ingestChainRepository;
    private final DumpSettingsService dumpSettingsService;
    private final AIPNotificationSettingsService notificationSettingsService;

    public IngestConfigurationManager(IIngestProcessingChainService processingService,
                                      IIngestProcessingChainRepository ingestChainRepository,
                                      DumpSettingsService dumpSettingsService,
                                      AIPNotificationSettingsService notificationSettingsService
    ) {
        this.processingService = processingService;
        this.ingestChainRepository = ingestChainRepository;
        this.dumpSettingsService = dumpSettingsService;
        this.notificationSettingsService = notificationSettingsService;
    }

    @Override
    public Set<String> importConfiguration(ModuleConfiguration configuration) {

        Set<String> importErrors = new HashSet<>();
        List<DynamicTenantSetting> dumpSettingList = dumpSettingsService.retrieve();
        List<DynamicTenantSetting> notificationSettingList = notificationSettingsService.retrieve();

        for (ModuleConfigurationItem<?> item : configuration.getConfiguration()) {
            if (IngestProcessingChain.class.isAssignableFrom(item.getKey())) {
                IngestProcessingChain ipc = item.getTypedValue();
                if (processingService.existsChain(ipc.getName())) {
                    importErrors.add(String.format(
                            "Ingest processing chain already exists with same name, skipping import of %s.",
                            ipc.getName()
                    ));
                } else {
                    try {
                        processingService.createNewChain(ipc);
                    } catch (ModuleException e) {
                        importErrors.add(String.format("Skipping import of IngestProcessingChain %s: %s", ipc.getName(), e.getMessage()));
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            } else if (DynamicTenantSetting.class.isAssignableFrom(item.getKey())) {
                DynamicTenantSetting setting = item.getTypedValue();
                String settingName = setting.getName();
                try {
                    if (dumpSettingList.stream().anyMatch(s -> s.getName().equals(settingName))) {
                        dumpSettingsService.update(setting);
                    } else if (notificationSettingList.stream().anyMatch(s -> s.getName().equals(settingName))) {
                        notificationSettingsService.update(setting);
                    } else {
                        importErrors.add(String.format("Configuration item not imported : Unknown Tenant Setting %s", setting));
                        LOGGER.error("Configuration item not imported : Unknown Tenant Setting {}", setting);
                    }
                } catch (ModuleException e) {
                    importErrors.add(String.format("Configuration item not imported : Invalid Tenant Setting %s", setting));
                    LOGGER.error("Configuration item not imported : Invalid Tenant Setting {}", setting);
                }
            }
        }
        return importErrors;
    }

    @Override
    public ModuleConfiguration exportConfiguration() {
        List<ModuleConfigurationItem<?>> configuration = new ArrayList<>();
        ingestChainRepository.findAll()
                .forEach(ipc -> configuration.add(ModuleConfigurationItem.build(ipc)));
        dumpSettingsService.retrieve()
                .forEach(setting -> configuration.add(ModuleConfigurationItem.build(setting)));
        notificationSettingsService.retrieve()
                .forEach(setting -> configuration.add(ModuleConfigurationItem.build(setting)));
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
