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
package fr.cnes.regards.modules.ingest.service.conf;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractModuleManagerWithTenantSettings;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.service.chain.IIngestProcessingChainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Configuration manager for current module
 *
 * @author Marc Sordi
 */
@Component
public class IngestConfigurationManager extends AbstractModuleManagerWithTenantSettings<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestConfigurationManager.class);

    private final IIngestProcessingChainService processingService;

    private final IIngestProcessingChainRepository ingestChainRepository;

    public IngestConfigurationManager(IIngestProcessingChainService processingService,
                                      IIngestProcessingChainRepository ingestChainRepository) {
        this.processingService = processingService;
        this.ingestChainRepository = ingestChainRepository;
    }

    @Override
    public Set<String> importConfiguration(ModuleConfiguration configuration, Set<String> importErrors) {
        Set<IngestProcessingChain> ingestProcessingChainSettings = getConfigurationSettingsByClass(configuration,
                                                                                                   IngestProcessingChain.class);
        importErrors.addAll(importIngestProcessingChainConfs(ingestProcessingChainSettings));

        return importErrors;
    }

    private Collection<String> importIngestProcessingChainConfs(Set<IngestProcessingChain> ingestProcessingChainSettings) {
        Set<String> errors = Sets.newHashSet();

        for (IngestProcessingChain ipc : ingestProcessingChainSettings) {
            if (processingService.existsChain(ipc.getName())) {
                errors.add(String.format("Ingest processing chain already exists with same name, skipping import of %s.",
                                         ipc.getName()));
            } else {
                try {
                    processingService.createNewChain(ipc);
                } catch (ModuleException e) {
                    errors.add(String.format("Skipping import of IngestProcessingChain %s: %s",
                                             ipc.getName(),
                                             e.getMessage()));
                }
            }
        }
        return errors;
    }

    @Override
    public ModuleConfiguration exportConfiguration(List<ModuleConfigurationItem<?>> configuration) {
        ingestChainRepository.findAll().forEach(ipc -> configuration.add(ModuleConfigurationItem.build(ipc)));
        return ModuleConfiguration.build(info, configuration);
    }

    @Override
    protected Set<String> resetConfiguration(Set<String> errors) {
        // Ingest chains cannot be removed easily
        return errors;
    }
}
