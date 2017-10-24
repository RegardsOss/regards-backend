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

package fr.cnes.regards.modules.acquisition.service.step;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionFileService;
import fr.cnes.regards.modules.acquisition.service.IChainGenerationService;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionException;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionRuntimeException;

/**
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class GenerateSipStep extends AbstractStep implements IGenerateSipStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateSipStep.class);

    @Autowired
    IPluginService pluginService;

    @Autowired
    IChainGenerationService chainGenerationService;

    @Autowired
    IAcquisitionFileRepository acquisitionFileRepository;

    @Autowired
    IAcquisitionFileService acquisitionFileService;

    private ChainGeneration chainGeneration;

    /**
     * {@link List} of {@link AcquisitionFile} that should be check
     */
    private List<AcquisitionFile> validFileList;

    @Override
    public void proceedStep() throws AcquisitionRuntimeException {

        this.chainGeneration = process.getChainGeneration();

        // A plugin for the generate Sip configuration is required
        if (this.chainGeneration.getGenerateSIPPluginConf() == null) {
            throw new RuntimeException("The required IGenerateSipStep is missing for the ChainGeneration <"
                    + this.chainGeneration.getLabel() + ">");
        }

        // Lunch the generate plugin
        try {
            // build the plugin parameters
            PluginParametersFactory factory = PluginParametersFactory.build();
            for (Map.Entry<String, String> entry : this.chainGeneration.getGenerateSIPParameter().entrySet()) {
                factory.addParameterDynamic(entry.getKey(), entry.getValue());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Add parameter <{}> with value : {}", entry.getKey(), entry.getValue());
                }
            }

            // get an instance of the plugin
            IGenerateSIPPlugin generateSipPlugin = pluginService
                    .getPlugin(this.chainGeneration.getGenerateSIPPluginConf(),
                               factory.getParameters().toArray(new PluginParameter[factory.getParameters().size()]));

            if (validFileList != null && validFileList.size() > 0) {
                
                // TODO CMZ ici regrouper par produit
                //
                
                generateSipPlugin.createMetaDataPlugin(validFileList);
            }

        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    @Override
    public void getResources() throws AcquisitionException {
        validFileList = new ArrayList<>();
        for (MetaFile metaFile : process.getChainGeneration().getMetaProduct().getMetaFiles()) {
            validFileList
                    .addAll(acquisitionFileRepository.findByStatusAndMetaFile(AcquisitionFileStatus.VALID, metaFile));
        }
    }

    @Override
    public void freeResources() throws AcquisitionException {
    }

    @Override
    public void stop() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void sleep() {
    }

    @Override
    public String getName() {
        return this.getClass().getCanonicalName();
    }

}
