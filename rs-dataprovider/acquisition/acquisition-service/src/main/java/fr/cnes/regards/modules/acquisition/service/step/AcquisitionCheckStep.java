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
import org.springframework.stereotype.Service;

import com.google.common.io.Files;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.ErrorType;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionFileService;
import fr.cnes.regards.modules.acquisition.service.IChainGenerationService;
import fr.cnes.regards.modules.acquisition.service.IProductService;
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

    private static String SUFFIX_FOR_INVALID_FILE = ".inv";

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IChainGenerationService chainGenerationService;

    @Autowired
    private IAcquisitionFileRepository acquisitionFileRepository;

    @Autowired
    private IAcquisitionFileService acquisitionFileService;

    @Autowired
    private IProductService productService;

    @Value("${regards.acquisition.invalid-data-folder:#{null}}")
    private String invalidDataFolder;

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
                    .getPlugin(this.chainGeneration.getCheckAcquisitionPluginConf(),
                               factory.getParameters().toArray(new PluginParameter[factory.getParameters().size()]));

            if (inProgressFileList != null) {
                // for each AcquisitionFile
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
                        acqFile.setStatus(AcquisitionFileStatus.VALID);
                    } else {
                        acqFile.setStatus(AcquisitionFileStatus.INVALID);
                    }

                    // Check file status and link the AcquisitionFile to the Product
                    Product product = checkFileStatus(acqFile, currentFile, checkPlugin.getProductName());

                    synchronizedDatabase(acqFile, product);
                }
            }

        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    private Product linkAcquisitionFileToProduct(AcquisitionFile acqFile, String productName) {
        // Get the product if it exists
        Product currentProduct = productService.retrive(productName);

        if (currentProduct == null) {
            // It is a new Product,  create it
            currentProduct = new Product();
            currentProduct.setProductName(productName);
            currentProduct.setStatus(ProductStatus.ACQUIRING);
            currentProduct.setMetaProduct(process.getChainGeneration().getMetaProduct());
        }

        currentProduct.addAcquisitionFile(acqFile);
        acqFile.setProduct(currentProduct);
        //    currentProduct.setVersion(checkPlugin.getProductVersion()); TODO CMZ virer 
        //    acqFile.setNodeIdentifier(checkPlugin.getNodeIdentifier()); TODO CMZ à virer

        return currentProduct;

    }

    private Product checkFileStatus(AcquisitionFile acqFile, File currentFile, String productName)
            throws ModuleException {
        Product product = null;
        if (acqFile.getStatus().equals(AcquisitionFileStatus.VALID)) {
            LOGGER.info("Valid file {}", acqFile.getFileName());

            // Report status
            //            process_.addEventToReport(AcquisitionMessages.getInstance()
            //                    .getMessage("ssalto.service.acquisition.run.step.check.valid.file", currentFile.getFileName()));

            // Link valid file to product
            product = linkAcquisitionFileToProduct(acqFile, productName);

        } else if (acqFile.getStatus().equals(AcquisitionFileStatus.INVALID)) {

            LOGGER.info("Invalid file {}", acqFile.getFileName());

            // Report status
            //                process_.addEventToReport(AcquisitionMessages
            //                        .getInstance()
            //                        .getMessage("ssalto.service.acquisition.run.step.check.invalid.file", currentFile.getFileName()));

            // Set error
            acqFile.getAcquisitionInformations().setError(ErrorType.ERROR);

            // Set process status
            // process.setProcessWarningStatus();

            // Move invalid file in a dedicated directory
            moveInvalidFile(acqFile, currentFile);

        } else {
            LOGGER.error("Invalid status for file {}", acqFile.getFileName());

            //                // Report error
            //                // Do not throw error : check step must not be blocked
            //                process_.addErrorToReport(AcquisitionMessages.getInstance()
            //                        .getMessage("ssalto.service.acquisitionun.step..rcheck.unexpected.status",
            //                                    currentFile.getFileName()));
        }

        return product;
    }

    private void moveInvalidFile(AcquisitionFile acqFile, File currentFile) throws ModuleException {
        // Create invalid folder (if necessary)
        final File invalidFolder = new File(
                this.invalidDataFolder + File.separator + acqFile.getMetaFile().getInvalidFolder());
        // Rename file with suffix
        final File targetFile = new File(invalidFolder, acqFile.getFileName() + SUFFIX_FOR_INVALID_FILE);

        try {
            Files.createParentDirs(targetFile);
            Files.move(currentFile, targetFile);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    private void synchronizedDatabase(AcquisitionFile acqFile, Product product) {
        if (product != null) {
            productService.save(product);
        }

        acquisitionFileService.save(acqFile);
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
