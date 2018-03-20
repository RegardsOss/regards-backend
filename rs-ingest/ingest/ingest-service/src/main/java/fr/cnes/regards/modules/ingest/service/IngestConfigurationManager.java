/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.manager.AbstractModuleConfigurationManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.service.chain.IIngestProcessingService;

/**
 * Configuration manager for current module
 * @author Marc Sordi
 */
@Service
public class IngestConfigurationManager extends AbstractModuleConfigurationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestConfigurationManager.class);

    @Autowired
    private IIngestProcessingService processingService;

    @Autowired
    private IIngestProcessingChainRepository ingestChainRepository;

    @Override
    public void importConfiguration(ModuleConfiguration configuration) throws ModuleException {
        for (ModuleConfigurationItem<?> item : configuration.getConfiguration()) {
            if (IngestProcessingChain.class.isAssignableFrom(item.getKey())) {
                IngestProcessingChain ipc = item.getTypedValue();
                if (processingService.existsChain(ipc.getName())) {
                    LOGGER.warn("Ingest processing chain already exists with same name, skipping import of {}.",
                                ipc.getName());
                    // FIXME notify
                } else {
                    processingService.createNewChain(ipc);
                }
            }
        }
    }

    @Override
    public ModuleConfiguration exportConfiguration() {
        List<ModuleConfigurationItem<?>> configuration = new ArrayList<>();
        for (IngestProcessingChain ipc : ingestChainRepository.findAll()) {
            configuration.add(ModuleConfigurationItem.build(ipc));
        }
        return ModuleConfiguration.build(info, configuration);
    }
}
