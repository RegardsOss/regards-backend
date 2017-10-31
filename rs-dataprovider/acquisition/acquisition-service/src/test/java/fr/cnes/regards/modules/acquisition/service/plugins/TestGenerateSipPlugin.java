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

import java.net.MalformedURLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaProductDto;
import fr.cnes.regards.modules.acquisition.domain.metamodel.MetaAttribute;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.domain.model.DateTimeAttribute;
import fr.cnes.regards.modules.acquisition.domain.model.LongAttribute;
import fr.cnes.regards.modules.acquisition.domain.model.StringAttribute;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanDirectoryPlugin;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionException;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPCollectionBuilder;

/**
 * A default {@link Plugin} of type {@link IAcquisitionScanDirectoryPlugin}.
 *
 * @author Christophe Mertz
 */
@Plugin(id = "TestGenerateSipPlugin", version = "1.0.0-SNAPSHOT", description = "TestGenerateSipPlugin",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class TestGenerateSipPlugin implements IGenerateSIPPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestGenerateSipPlugin.class);

    public static final String CHAIN_GENERATION_PARAM = "chain-label";

    public static final String SESSION_PARAM = "session-param";

    public static final String META_PRODUCT_PARAM = "meta-produt";

    //    @Autowired
    //    private IMetaFileService metaFileService;

    @PluginParameter(name = CHAIN_GENERATION_PARAM, optional = true)
    private String chainLabel;

    @PluginParameter(name = SESSION_PARAM, optional = true)
    private String session;

    @PluginParameter(name = META_PRODUCT_PARAM, optional = true)
    private MetaProductDto metaProductDto;

    @Override
    public SortedMap<Integer, Attribute> createMetadataPlugin(List<AcquisitionFile> acqFiles, String datasetName)
            throws ModuleException {
        return createMetadataPlugin(acqFiles, datasetName);
    }

    @Override
    public SortedMap<Integer, Attribute> createMetaDataPlugin(List<AcquisitionFile> acqFiles) throws ModuleException {
        LOGGER.info("Start create MetaData for the chain <{}> ", chainLabel);

        String productName = acqFiles.get(0).getProduct().getProductName();

        LOGGER.info("product name <{}> ", productName);

        SortedMap<Integer, Attribute> attributeMap = new TreeMap<>();
        attributeMap.put(0, createLongAttribute("orbit", 100));
        attributeMap.put(0, createLongAttribute("order", 133));
        attributeMap.put(0, createStringAttribute("comment", "Hello Toulouse"));
        attributeMap.put(0, createDateAttribute("creation date", OffsetDateTime.now()));

        LOGGER.info("End create Metata for the chain <{}>", chainLabel);

        return attributeMap;
    }

    private StringAttribute createStringAttribute(String name, String val) {
        StringAttribute attr = new StringAttribute();
        attr.setMetaAttribute(new MetaAttribute(name, AttributeTypeEnum.TYPE_STRING));
        attr.addValue(val);
        return attr;
    }

    private LongAttribute createLongAttribute(String name, int val) {
        LongAttribute attr = new LongAttribute();
        attr.setMetaAttribute(new MetaAttribute(name, AttributeTypeEnum.TYPE_LONG_STRING));
        attr.addValue(val);
        return attr;
    }

    private DateTimeAttribute createDateAttribute(String name, OffsetDateTime val) {
        DateTimeAttribute attr = new DateTimeAttribute();
        attr.setMetaAttribute(new MetaAttribute(name, AttributeTypeEnum.TYPE_DATE_TIME));
        attr.addValue(val);
        return attr;
    }

    @Override
    public SIPCollection runPlugin(String sessionId, List<AcquisitionFile> acqFiles, String datasetName)
            throws ModuleException {
        SIPCollection sipCollection = runPlugin(sessionId, acqFiles);
        // TODO CMZ ajouter le tag vers le jeu datasetName
        return sipCollection;
    }

    @Override
    public SIPCollection runPlugin(String sessionId, List<AcquisitionFile> acqFiles) throws ModuleException {
        String productName = acqFiles.get(0).getProduct().getProductName();

        LOGGER.info("Start SIP generation for product <{}>", productName);

        // TODO CMZ il faut avoir récupérer le processingChain au microservice Ingest
        SIPCollectionBuilder sipCollectionBuilder = new SIPCollectionBuilder("processingChain", this.session);

        SIPBuilder sipBuilder = new SIPBuilder(productName);

        for (AcquisitionFile af : acqFiles) {
            try {
                sipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, af.getFile().toURI().toURL(),
                                                                        af.getChecksum(), af.getChecksumAlgorithm());
                sipBuilder.addContentInformation();
            } catch (MalformedURLException e) {
                LOGGER.error(e.getMessage(), e);
                throw new AcquisitionException(e.getMessage());
            }
        }

        SortedMap<Integer, Attribute> mm = this.createMetaDataPlugin(acqFiles);

        SIP aSip = sipBuilder.build();
        if (LOGGER.isDebugEnabled()) {
            Gson gson = new Gson();
            LOGGER.debug(gson.toJson(aSip));
        }

        sipCollectionBuilder.add(aSip);

        LOGGER.info("End SIP generation for product <{}>", productName);

        return sipCollectionBuilder.build();
    }

}
