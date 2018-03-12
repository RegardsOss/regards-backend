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
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.oais.builder.PDIBuilder;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.plugins.ISIPGenerationPluginWithMetadataToolbox;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;

/**
 * Common SIP generation tools
 *
 * @author Christophe Mertz
 *
 */
public abstract class AbstractGenerateSIPPlugin extends AbstractMiscStorageInformation
        implements ISIPGenerationPluginWithMetadataToolbox {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGenerateSIPPlugin.class);

    private static final String PRODUCT_NAME = "product_name";

    public static final String DATASET_SIP_ID = "datasetSipId";

    @PluginParameter(name = DATASET_SIP_ID, label = "Dataset provider identifier (i.e. SIP ID)",
            description = "Also used for plugin configuration retrieval")
    protected String datasetName;

    @Override
    public SIP generate(Product product) throws ModuleException {

        LOGGER.info("Start SIP generation for product <{}>", product.getProductName());

        // Init the builder
        SIPBuilder sipBuilder = new SIPBuilder(product.getProductName());

        sipBuilder.addDescriptiveInformation(PRODUCT_NAME, product.getProductName());

        // Add all AcquisistionFile to the content information
        addDataObjectsToSip(sipBuilder, product.getActiveAcquisitionFiles());

        // Extracts the meta-attributes
        SortedMap<Integer, Attribute> mm = createMetadataPlugin(product.getAcquisitionFiles());

        addAttributesTopSip(sipBuilder, mm);

        // Add misc information
        addMiscStorageInfomation(sipBuilder);

        // Add the SIP to the SIPCollection
        SIP aSip = sipBuilder.build();

        addDatasetTag(aSip, datasetName);

        if (LOGGER.isDebugEnabled()) {
            Gson gson = new Gson();
            LOGGER.debug(gson.toJson(aSip));
        }

        LOGGER.info("End SIP generation for product <{}>", product.getProductName());

        return aSip;
    }

    protected void addDatasetTag(SIP aSip, String datasetSipId) {
        // If a dataSet is defined, add a tag to the PreservationDescriptionInformation
        PDIBuilder pdiBuilder = new PDIBuilder(aSip.getProperties().getPdi());
        pdiBuilder.addTags(datasetSipId);
        aSip.getProperties().setPdi(pdiBuilder.build());
    }

    protected abstract void addAttributesTopSip(SIPBuilder sipBuilder, SortedMap<Integer, Attribute> mapAttrs)
            throws ModuleException;

    protected abstract void addDataObjectsToSip(SIPBuilder sipBuilder, List<AcquisitionFile> acqFiles)
            throws ModuleException;
}
