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
package fr.cnes.regards.modules.acquisition.service;

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
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;

/**
 * Configuration manager for current module
 * @author Marc Sordi
 */
@Service
public class AcquisitionConfigurationManager extends AbstractModuleConfigurationManager {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionConfigurationManager.class);

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Override
    public void importConfiguration(ModuleConfiguration configuration) throws ModuleException {
        for (ModuleConfigurationItem<?> item : configuration.getConfiguration()) {
            if (AcquisitionProcessingChain.class.isAssignableFrom(item.getKey())) {
                AcquisitionProcessingChain apc = item.getTypedValue();
                // Inactive chain at the moment FIXME
                apc.setActive(Boolean.FALSE);
                processingService.createChain(apc);
            }
        }
    }

    @Override
    public ModuleConfiguration exportConfiguration() throws ModuleException {
        List<ModuleConfigurationItem<?>> configuration = new ArrayList<>();
        for (AcquisitionProcessingChain apc : processingService.getFullChains()) {
            configuration.add(ModuleConfigurationItem.build(apc));
        }
        return ModuleConfiguration.build(info, configuration);
    }

}
