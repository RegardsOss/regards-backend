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
package fr.cnes.regards.modules.acquisition.service.plugins;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.builder.PDIBuilder;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionException;
import fr.cnes.regards.modules.entities.client.IDatasetClient;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;

/**
 *
 * @author Christophe Mertz
 *
 */
public abstract class AbstractGenerateSIPPlugin implements IGenerateSIPPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGenerateSIPPlugin.class);

    @Autowired
    private IDatasetClient datasetClient;

    @Override
    public SIP generate(Product product) throws ModuleException {

        LOGGER.info("Start SIP generation for product <{}>", product.getProductName());

        // Init the builder
        SIPBuilder sipBuilder = new SIPBuilder(product.getProductName());

        // Add all AcquisistionFile to the content information
        addDataObjectsToSip(sipBuilder, product.getAcquisitionFiles());

        // Get the dataset name
        String datasetName = datasetClient.retrieveDataset(product.getProcessingChain().getDatasetIpId()).getBody()
                .getContent().getSipId();

        // Extracts the meta-attributes
        SortedMap<Integer, Attribute> mm = createMetadataPlugin(product.getAcquisitionFiles(), datasetName);

        addAttributesTopSip(sipBuilder, mm);

        // Add the SIP to the SIPCollection
        SIP aSip = sipBuilder.build();

        // If a dataSet is defined, add a tag to the PreservationDescriptionInformation
        addDatasetTag(aSip, Optional.of(product.getProcessingChain().getDatasetIpId()));

        if (LOGGER.isDebugEnabled()) {
            Gson gson = new Gson();
            LOGGER.debug(gson.toJson(aSip));
        }

        LOGGER.info("End SIP generation for product <{}>", product.getProductName());

        return aSip;
    }

    protected void addDatasetTag(SIP aSip, Optional<String> datasetIpId) {
        // If a dataSet is defined, add a tag to the PreservationDescriptionInformation
        if (datasetIpId.isPresent()) {
            PDIBuilder pdiBuilder = new PDIBuilder(aSip.getProperties().getPdi());
            pdiBuilder.addTags(datasetIpId.get());
            aSip.getProperties().setPdi(pdiBuilder.build());
        }
    }

    protected abstract void addAttributesTopSip(SIPBuilder sipBuilder, SortedMap<Integer, Attribute> mapAttrs)
            throws AcquisitionException;

    protected abstract void addDataObjectsToSip(SIPBuilder sipBuilder, List<AcquisitionFile> acqFiles)
            throws AcquisitionException;

    protected abstract SortedMap<Integer, Attribute> createMetadataPlugin(List<AcquisitionFile> acqFiles,
            String datasetName) throws ModuleException;
}
