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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionFileService;
import fr.cnes.regards.modules.acquisition.service.IChainGenerationService;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionException;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionRuntimeException;
import fr.cnes.regards.modules.ingest.client.IIngestClient;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;

/**
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class GenerateSipStep extends AbstractStep implements IGenerateSipStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateSipStep.class);

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IAcquisitionFileService acquisitionFileService;

    @Autowired
    private IProductService productService;

    @Autowired
    private IIngestClient ingestClient;

    private ChainGeneration chainGeneration;

    @Value("${regards.acquisition.sip.max.bulk.size:5000}")
    private int sipCollectionBulkMaxSize;

    private SIPCollection sipCollection;

    /**
     * The {@link List} of {@link AcquisitionFile} that should be check grouped by {@link Product} id
     */
    private Map<Long, List<AcquisitionFile>> afMap;

    @Override
    public void proceedStep() throws AcquisitionRuntimeException {

        this.chainGeneration = process.getChainGeneration();

        if (this.afMap.isEmpty()) {
            LOGGER.info("Any file to process for the acquisition chain <{}>", this.chainGeneration.getLabel());
            return;
        }

        // A plugin for the generate SIP configuration is required
        if (this.chainGeneration.getGenerateSIPPluginConf() == null) {
            throw new RuntimeException("The required IGenerateSipStep is missing for the ChainGeneration <"
                    + this.chainGeneration.getLabel() + ">");
        }

        // Launch the generate plugin
        try {
            // build the plugin parameters
            PluginParametersFactory factory = PluginParametersFactory.build();
            for (Map.Entry<String, String> entry : this.chainGeneration.getGenerateSIPParameter().entrySet()) {
                factory.addParameterDynamic(entry.getKey(), entry.getValue());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("[{}] Add parameter <{}> with value : {}", chainGeneration.getSession(),
                                 entry.getKey(), entry.getValue());
                }
            }

            // get an instance of the plugin
            IGenerateSIPPlugin generateSipPlugin = pluginService
                    .getPlugin(this.chainGeneration.getGenerateSIPPluginConf().getId(),
                               factory.getParameters().toArray(new PluginParameter[factory.getParameters().size()]));

            // create MetaData for each Product
            this.afMap.forEach((k, af) -> {
                try {
                    this.sipCollection = generateSipPlugin.runPlugin(af, Optional.of(chainGeneration.getDataSet()));
                    processSipCollection();
                } catch (ModuleException e) {
                    LOGGER.error(e.getMessage(), e);
                    throw new AcquisitionRuntimeException(e.getMessage());
                }
            });

            publishSipCollections();

        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            throw new AcquisitionRuntimeException(e.getMessage());
        }

    }

    /**
     * If the number of SIP in the {@link SIPCollection} is greater than {@link GenerateSipStep#sipCollectionBulkMaxSize},</br>
     * the {@link SIPCollection} is publish to the ingest microservice 
     * @throws ModuleException 
     */
    private void processSipCollection() throws ModuleException {
        if (this.sipCollection.getFeatures().size() >= sipCollectionBulkMaxSize) {
            publishSipCollections();
        }
    }

    /**
     * Send the {@link SIPCollection} to Ingest microservice 
     * @throws ModuleException 
     */
    private void publishSipCollections() throws ModuleException {
        LOGGER.info("[{}] Start publish SIP Collections", chainGeneration.getSession());

        ResponseEntity<Collection<SIPEntity>> response = ingestClient.ingest(this.sipCollection);

        // TODO CMZ en fonction du retour, il faudrait repercuter l'état sur le processing 
        if (response.getStatusCode().equals(HttpStatus.CREATED)) {
            manageSipPublishCreated();
        } else if (response.getStatusCode().equals(HttpStatus.PARTIAL_CONTENT)) {
            manageSipPublishPartialContent(response.getBody());
        } else if (response.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
            LOGGER.error("[{}] Unauthorized access to ingest microservice", chainGeneration.getSession());

        } else if (response.getStatusCode().equals(HttpStatus.CONFLICT)) {
            LOGGER.error("[{}] Unauthorized access to ingest microservice", chainGeneration.getSession());

        }
        this.sipCollection.getFeatures().clear();

        LOGGER.info("[{}] End publish SIP Collections", chainGeneration.getSession());
    }

    private void manageSipPublishCreated() throws ModuleException {
        LOGGER.info("[{}] SIP collection has heen processed with success : {} ingested", chainGeneration.getSession(),
                    this.sipCollection.getFeatures().size());
        for (SIP sip : this.sipCollection.getFeatures()) {
            updateProductStatus(sip.getId(), ProductStatus.FINISHED);
        }
    }

    private void manageSipPublishPartialContent(Collection<SIPEntity> sipEntitys) throws ModuleException {
        LOGGER.error("[{}] SIP collection has heen partially processed with success : {} ingested / {} rejected",
                     chainGeneration.getSession(), this.sipCollection.getFeatures().size() - sipEntitys.size(),
                     sipEntitys.size());
        Set<String> sipIdError = new HashSet<>();
        for (SIPEntity sipEntity : sipEntitys) {
            LOGGER.error("[{}] SIP in error : productName=<{}>, raeson=<{}>", chainGeneration.getSession(),
                         sipEntity.getSipId(), sipEntity.getReasonForRejection());
            updateProductStatus(sipEntity.getSipId(), ProductStatus.ERROR);
            sipIdError.add(sipEntity.getSipId());
        }

        for (SIP sip : this.sipCollection.getFeatures()) {
            if (!sipIdError.contains(sip.getId())) {
                updateProductStatus(sip.getId(), ProductStatus.FINISHED);
            }
        }
    }

    private void updateProductStatus(String sipId, ProductStatus status) throws ModuleException {
        Product product = productService.retrieve(sipId);
        if (product == null) {
            final StringBuffer strBuff = new StringBuffer();
            strBuff.append("The product name <");
            strBuff.append(sipId);
            strBuff.append("> does not exist");
            ModuleException ex = new ModuleException(strBuff.toString());
            LOGGER.error(ex.getMessage());
            throw ex;
        }
        product.setStatus(status);
        productService.save(product);
    }

    @Override
    public void getResources() throws AcquisitionException {
        this.afMap = new HashMap<Long, List<AcquisitionFile>>();
        
        // Get the VALID AcquisitionFile
        List<AcquisitionFile> validFileList = new ArrayList<>();
        for (MetaFile metaFile : process.getChainGeneration().getMetaProduct().getMetaFiles()) {
            validFileList.addAll(acquisitionFileService.findByStatusAndMetaFile(AcquisitionFileStatus.VALID, metaFile));
        }

        // Get all the products from the VALID AcquisitionFile
        Set<Product> products = new HashSet<Product>();
        validFileList.forEach(af -> {
            if (!products.contains(af.getProduct())) {
                products.add(af.getProduct());
            }
        });

        // Get the AcquisitionFiles for each Product
        for (Product pr : products) {
            if (pr.getStatus().equals(ProductStatus.COMPLETED) || pr.getStatus().equals(ProductStatus.FINISHED)) {
                List<AcquisitionFile> afs = new ArrayList<>();
                validFileList.stream().filter(af -> af.getProduct().equals(pr)).forEach(af -> afs.add(af));
                this.afMap.put(pr.getId(), afs);
            }
        }
    }

    @Override
    public void freeResources() throws AcquisitionException {
        this.afMap = null;
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
