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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionFileService;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionException;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionRuntimeException;

/**
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
public class GenerateSipStep extends AbstractStep implements IGenerateSipStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateSipStep.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IAcquisitionFileService acquisitionFileService;

    @Autowired
    private IProductService productService;

    private ChainGeneration chainGeneration;

    private Product product;

    @Value("${regards.acquisition.sip.max.bulk.size:5000}")
    private int sipCollectionBulkMaxSize;

    /**
     * The List of {@link AcquisitionFile} for the current {@link Product}
     */
    private List<AcquisitionFile> acqFiles;

    @Override
    public void proceedStep() throws AcquisitionRuntimeException, AcquisitionException {

        if (chainGeneration == null) {
            String msg = "The chain generation is mandatory";
            LOGGER.error(msg);
            throw new AcquisitionRuntimeException(msg);
        }

        if (product == null) {
            throw new AcquisitionException("The product is mandatory");
        }

        LOGGER.info("[{}] Start generate SIP step for the product <{}>", chainGeneration.getSession(),
                    product.getProductName());

        if (this.acqFiles.isEmpty()) {
            LOGGER.info("Any file to process for the acquisition chain <{}>", this.chainGeneration.getLabel());
            return;
        }

        // A plugin for the generate SIP configuration is required
        if (this.chainGeneration.getGenerateSipPluginConf() == null) {
            throw new AcquisitionException(
                    "[" + this.chainGeneration.getLabel() + "] The required IGenerateSIPPlugin is missing");
        }

        // Launch the generate plugin
        try {
            // build the plugin parameters
            PluginParametersFactory factory = PluginParametersFactory.build();
            for (Map.Entry<String, String> entry : this.chainGeneration.getGenerateSipParameter().entrySet()) {
                factory.addParameterDynamic(entry.getKey(), entry.getValue());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("[{}] Add parameter <{}> with value : {}", chainGeneration.getSession(),
                                 entry.getKey(), entry.getValue());
                }
            }

            // get an instance of the plugin
            IGenerateSIPPlugin generateSipPlugin = pluginService
                    .getPlugin(this.chainGeneration.getGenerateSipPluginConf().getId(),
                               factory.getParameters().toArray(new PluginParameter[factory.getParameters().size()]));

            // TODO CMZ attention à ne calculer le SIP que si c'est nécessaire
            // si pas saved dans ingest, mais avec le SIP déjà calculé, il faut essayer de l'envoyer sans le recalculer
            // Calc the SIP and save the Product
            product.setSip(generateSipPlugin.runPlugin(this.acqFiles, Optional.of(chainGeneration.getDataSet())));
            productService.save(product);

        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            throw new AcquisitionException(e.getMessage());
        }

        LOGGER.info("[{}] Stop  generate SIP step for the product <{}>", chainGeneration.getSession(),
                    product.getProductName());
    }

    @Override
    public void getResources() throws AcquisitionException {
        this.chainGeneration = process.getChainGeneration();
        this.product = process.getProduct();
        this.acqFiles = acquisitionFileService.findByProduct(this.product);
    }

    @Override
    public void freeResources() throws AcquisitionException { // NOSONAR
    }

    @Override
    public void stop() { // NOSONAR
    }

}
