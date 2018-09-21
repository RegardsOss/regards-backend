/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.module.manager.AbstractModuleManager;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.module.manager.ModuleReadinessReport;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;

/**
 * Configuration manager for current module
 * @author Marc Sordi
 */
@Service
public class AcquisitionConfigurationManager extends AbstractModuleManager<Void> {

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Override
    public Set<String> importConfiguration(ModuleConfiguration configuration) {
        Set<String> importErrors = new HashSet<>();
        for (ModuleConfigurationItem<?> item : configuration.getConfiguration()) {
            if (AcquisitionProcessingChain.class.isAssignableFrom(item.getKey())) {
                AcquisitionProcessingChain apc = item.getTypedValue();
                // Inactive chain at the moment
                apc.setActive(Boolean.FALSE);
                try {
                    processingService.createChain(apc);
                } catch (ModuleException e) {
                    importErrors.add(String.format("Skipping import of chain with label %s: %s", apc.getLabel(),
                                                   e.getMessage()));
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return importErrors;
    }

    @Override
    public ModuleConfiguration exportConfiguration() throws ModuleException {
        List<ModuleConfigurationItem<?>> configuration = new ArrayList<>();
        for (AcquisitionProcessingChain apc : processingService.getFullChains()) {
            configuration.add(ModuleConfigurationItem.build(apc));
        }
        return ModuleConfiguration.build(info, configuration);
    }

    @Override
    public ModuleReadinessReport<Void> isReady() {
        return new ModuleReadinessReport<Void>(true, null, null);
    }
}
