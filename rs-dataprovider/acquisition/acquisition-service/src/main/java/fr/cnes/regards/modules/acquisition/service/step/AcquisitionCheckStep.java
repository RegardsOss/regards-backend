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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;
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
public class AcquisitionCheckStep extends AbstractStep implements IAcquisitionCheckStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionCheckStep.class);

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
    private List<AcquisitionFile> inProgressFileList;

    @Override
    public void proceedStep() throws AcquisitionRuntimeException {

        this.chainGeneration = process.getChainGeneration();

        // A plugin for the scan configuration is required
        if (this.chainGeneration.getCheckAcquisitionPluginConf() == null) {
            throw new RuntimeException("The required IAcquisitionScanPlugin is missing for the ChainGeneration <"
                    + this.chainGeneration.getLabel() + ">");
        }

        // Lunch the scan plugin
        try {
            // build the plugin parameters
            PluginParametersFactory factory = PluginParametersFactory.build();
            for (Map.Entry<String, String> entry : this.chainGeneration.getCheckAcquisitionParameter().entrySet()) {
                factory.addParameterDynamic(entry.getKey(), entry.getValue());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Add <" + entry.getKey() + "> parameter " + entry.getValue() + " : ");
                }
            }

            // get an instance of the plugin
            ICheckFilePlugin checkPlugin = pluginService
                    .getPlugin(this.chainGeneration.getCheckAcquisitionPluginConf(),
                               factory.getParameters().toArray(new PluginParameter[factory.getParameters().size()]));

            if (inProgressFileList != null) {
                for (AcquisitionFile acqFile : inProgressFileList) {
                    File currentFile = null;
                    if (acqFile.getAcquisitionInformations() != null) {
                        String workingDir = acqFile.getAcquisitionInformations().getWorkingDirectory();
                        if (workingDir != null) {
                            currentFile = new File(workingDir, acqFile.getFileName());
                        } else {
                            currentFile = new File(acqFile.getAcquisitionInformations().getAcquisitionDirectory(),
                                    acqFile.getFileName());
                        }
                    } else {
                        currentFile = new File(acqFile.getFileName());
                    }

                    // execute the check plugin
                    if (checkPlugin.runPlugin(currentFile, chainGeneration.getDataSet())) {
                        // if the AcquisitionFile is check, update in database
                        synchronizedDatabase(acqFile, checkPlugin);
                    }
                }
            }

        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    private void synchronizedDatabase(AcquisitionFile acqFile, ICheckFilePlugin checkPlugin) {
        acqFile.setStatus(AcquisitionFileStatus.VALID);

        // TODO rattacher le fichier acquis au bon Produit
        // peut-être que le Produit existe déjà
        Product currentProduct = new Product();
        currentProduct.setProductName(checkPlugin.getProductName());
        currentProduct.setStatus(ProductStatus.ACQUIRING);
        //    currentProduct.setVersion(checkPlugin.getProductVersion()); TODO CMZ virer 
        acqFile.setProduct(currentProduct);
        //    acqFile.setNodeIdentifier(checkPlugin.getNodeIdentifier()); TODO CMZ à virer
    }

    @Override
    public void getResources() throws AcquisitionException {
        inProgressFileList = new ArrayList<>();
        for (MetaFile metaFile : process.getChainGeneration().getMetaProduct().getMetaFiles()) {
            inProgressFileList.addAll(acquisitionFileRepository
                    .findByStatusAndMetaFile(AcquisitionFileStatus.IN_PROGRESS, metaFile));
        }
    }

    @Override
    public void freeResources() throws AcquisitionException {
        inProgressFileList = null;
        super.process = null;
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
