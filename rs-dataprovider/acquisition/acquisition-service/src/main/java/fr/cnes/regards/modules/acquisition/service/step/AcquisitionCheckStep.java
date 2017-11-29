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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.io.Files;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionFileService;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionException;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionRuntimeException;

/**
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
public class AcquisitionCheckStep extends AbstractStep implements IAcquisitionCheckStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionCheckStep.class);

    private static String SUFFIX_FOR_INVALID_FILE = ".inv";

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IAcquisitionFileService acquisitionFileService;

    // TODO CMZ à revoir invalidDataFolder peut être différent par chaine de génération
    @Value("${regards.acquisition.invalid-data-folder:#{null}}")
    private String invalidDataFolder;

    private ChainGeneration chainGeneration;

    /**
     * {@link List} of {@link AcquisitionFile} that should be check
     */
    private List<AcquisitionFile> inProgressFileList;

    @Override
    public void proceedStep() throws AcquisitionRuntimeException, AcquisitionException {

        if (chainGeneration == null) {
            String msg = "The chain generation is mandatory";
            LOGGER.error(msg);
            throw new AcquisitionRuntimeException(msg);
        }

        if (inProgressFileList == null || inProgressFileList.isEmpty()) {
            LOGGER.info("Any file to process for the acquisition chain <{}>", this.chainGeneration.getLabel());
            return;
        }

        // A plugin for the scan configuration is required
        if (this.chainGeneration.getCheckAcquisitionPluginConf() == null) {
            throw new AcquisitionException(
                    "[" + this.chainGeneration.getLabel() + "] The required ICheckFilePlugin is missing");
        }

        // Lunch the check plugin
        try {
            // build the plugin parameters
            PluginParametersFactory factory = PluginParametersFactory.build();
            for (Map.Entry<String, String> entry : this.chainGeneration.getCheckAcquisitionParameter().entrySet()) {
                factory.addParameterDynamic(entry.getKey(), entry.getValue());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Add parameter <{}> with value : {}", entry.getKey(), entry.getValue());
                }
            }

            // get an instance of the plugin
            ICheckFilePlugin checkPlugin = pluginService
                    .getPlugin(this.chainGeneration.getCheckAcquisitionPluginConf().getId(),
                               factory.getParameters().toArray(new PluginParameter[factory.getParameters().size()]));

            // for each AcquisitionFile
            for (AcquisitionFile acqFile : inProgressFileList) {
                // execute the check plugin
                boolean result = checkPlugin.runPlugin(chainGeneration.getLabel(), acqFile.getFile(),
                                                       chainGeneration.getDataSet());

                // Check file status and link the AcquisitionFile to the Product
                acquisitionFileService.checkFileStatus(result, chainGeneration.getSession(), acqFile,
                                                       checkPlugin.getProductName(),
                                                       process.getChainGeneration().getMetaProduct(),
                                                       process.getChainGeneration().getMetaProduct().getIngestChain());

                if (acqFile.getStatus().equals(AcquisitionFileStatus.INVALID)) {
                    // Move invalid file in a dedicated directory
                    moveInvalidFile(acqFile);
                }
            }

        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    private void moveInvalidFile(AcquisitionFile acqFile) throws ModuleException {
        // Create invalid folder (if necessary)
        final File invalidFolder = new File(
                this.invalidDataFolder + File.separator + acqFile.getMetaFile().getInvalidFolder());
        // Rename file with suffix
        final File targetFile = new File(invalidFolder, acqFile.getFileName() + SUFFIX_FOR_INVALID_FILE);

        try {
            Files.createParentDirs(targetFile);
            Files.move(acqFile.getFile(), targetFile);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    @Override
    public void getResources() throws AcquisitionException {
        this.chainGeneration = process.getChainGeneration();

        this.inProgressFileList = new ArrayList<>();

        for (MetaFile metaFile : process.getChainGeneration().getMetaProduct().getMetaFiles()) {
            this.inProgressFileList.addAll(acquisitionFileService
                    .findByStatusAndMetaFile(AcquisitionFileStatus.IN_PROGRESS, metaFile));
        }
    }

    @Override
    public void freeResources() throws AcquisitionException { // NOSONAR
    }

    @Override
    public void stop() { // NOSONAR
    }

}
