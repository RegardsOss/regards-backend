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

package fr.cnes.regards.modules.acquisition.service.job.step;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.io.Files;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionFileService;
import fr.cnes.regards.modules.acquisition.service.job.ProductAcquisitionJob;
import fr.cnes.regards.modules.entities.client.IDatasetClient;

/**
 * @author Christophe Mertz
 *
 */
public class AcquisitionCheckStep extends AbstractDataProviderStep<Void, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionCheckStep.class);

    private static String SUFFIX_FOR_INVALID_FILE = ".inv";

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IAcquisitionFileService acquisitionFileService;

    @Autowired
    private IDatasetClient datasetClient;

    /**
     * Base folder used to move the invalid scan files
     */
    // FIXME not initialized
    private String invalidDataFolder;

    /**
     * {@link List} of {@link AcquisitionFile} that should be check
     */
    private List<AcquisitionFile> inProgressFileList;

    public AcquisitionCheckStep(ProductAcquisitionJob job) {
        super(job);
    }

    @Override
    protected Void doExecute(Void in) throws ProcessingStepException {

        LOGGER.debug("[{}] : check file step started.", acqProcessingChain.getLabel());

        // Retrieve in progress files per metafile
        inProgressFileList = new ArrayList<>();
        for (MetaFile metaFile : acqProcessingChain.getMetaProduct().getMetaFiles()) {
            inProgressFileList.addAll(acquisitionFileService.findByStatusAndMetaFile(AcquisitionFileState.IN_PROGRESS,
                                                                                     metaFile));
        }

        // Nothing to do if no file in progress
        if (inProgressFileList.isEmpty()) {
            LOGGER.info("[{}] : no file to process for the acquisition chain.", acqProcessingChain.getLabel());
            return null;
        }

        // Launch the check plugin
        try {
            // Build the plugin parameters
            PluginParametersFactory factory = PluginParametersFactory.build();
            for (Map.Entry<String, String> entry : this.acqProcessingChain.getCheckAcquisitionParameter().entrySet()) {
                factory.addParameter(entry.getKey(), entry.getValue());
                LOGGER.debug("[{}] : Add parameter <{}> with value : {}", acqProcessingChain.getLabel(), entry.getKey(),
                             entry.getValue());
            }

            // Get an instance of the plugin
            ICheckFilePlugin checkPlugin = pluginService
                    .getPlugin(acqProcessingChain.getCheckAcquisitionPluginConf().getId(),
                               factory.getParameters().toArray(new PluginParameter[factory.getParameters().size()]));

            String datasetName = datasetClient.retrieveDataset(acqProcessingChain.getDataSet()).getBody().getContent()
                    .getSipId();

            // for each AcquisitionFile
            for (AcquisitionFile acqFile : inProgressFileList) {
                // Execute the check plugin
                boolean result = checkPlugin.runPlugin(acqFile.getFile(), datasetName);

                // Check file status and link the AcquisitionFile to the Product
                acquisitionFileService.checkFileStatus(result, acqProcessingChain.getSession(), acqFile,
                                                       checkPlugin.getProductName(),
                                                       acqProcessingChain.getMetaProduct(),
                                                       acqProcessingChain.getMetaProduct().getIngestChain());

                if (acqFile.getStatus().equals(AcquisitionFileState.INVALID)) {
                    // Move invalid file in a dedicated directory
                    moveInvalidFile(acqFile);
                }
            }

        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
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
}
