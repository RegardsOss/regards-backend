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
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.netflix.servo.util.Strings;

import fr.cnes.regards.framework.geojson.geometry.Point;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.oais.PreservationDescriptionInformation;
import fr.cnes.regards.framework.oais.builder.PDIBuilder;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaProductDto;
import fr.cnes.regards.modules.acquisition.domain.metamodel.MetaAttribute;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.domain.model.DateTimeAttribute;
import fr.cnes.regards.modules.acquisition.domain.model.GeoAttribute;
import fr.cnes.regards.modules.acquisition.domain.model.LongAttribute;
import fr.cnes.regards.modules.acquisition.domain.model.StringAttribute;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanDirectoryPlugin;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.service.exception.AcquisitionException;
import fr.cnes.regards.modules.entities.domain.geometry.Crs;
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

    private static final String MISSION_ATTRIBUE = "Mission";

    private static final String INSTRUMENT_ATTRIBUE = "instrument";

    private static final String CREATION_DATE_ATTRIBUE = "creation date";

    private static final String GEO_POINT_ATTRIBUTE = "point coord";

    private static final Random random = new Random();

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

        int n = 0;
        SortedMap<Integer, Attribute> attributeMap = new TreeMap<>();
        attributeMap.put(n++, createLongAttribute("orbit", 100));
        attributeMap.put(n++, createLongAttribute("order", 133));
        attributeMap.put(n++, createStringAttribute(MISSION_ATTRIBUE, "Viking"));
        attributeMap.put(n++, createStringAttribute(INSTRUMENT_ATTRIBUE, "instrument de la mesure"));
        attributeMap.put(n++, createStringAttribute("comment", "Hello Toulouse"));
        attributeMap.put(n++, createDateAttribute(CREATION_DATE_ATTRIBUE, OffsetDateTime.now()));
        attributeMap.put(n++, createDateAttribute("start date", OffsetDateTime.now().minusMinutes(45)));
        attributeMap.put(n++, createDateAttribute("stop  date", OffsetDateTime.now().minusMinutes(15)));
        attributeMap
                .put(n++,
                     createPointAttribute(GEO_POINT_ATTRIBUTE, -90 * random.nextDouble(), 90 * random.nextDouble()));

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

    private GeoAttribute createPointAttribute(String name, Double latitude, Double longitude) {

        Point pp = new Point();
        Double[] bb = { latitude, longitude };
        pp.setBbox(bb);
        pp.setCrs(Crs.EARTH.toString());

        GeoAttribute attr = new GeoAttribute();
        attr.setMetaAttribute(new MetaAttribute(name, AttributeTypeEnum.TYPE_GEO_LOCATION));
        attr.addValue(pp);
        return attr;
    }

    @Override
    public SIPCollection runPlugin(List<AcquisitionFile> acqFiles, String datasetIpId) throws ModuleException {
        SIPCollection sipCollection = runPlugin(acqFiles);

        // If a  is defined, add a tag to the PreservationDescriptionInformation
        if (!Strings.isNullOrEmpty(datasetIpId)) {
            sipCollection.getFeatures().forEach(sip -> {
                PDIBuilder pdiBuilder = new PDIBuilder(sip.getProperties().getPdi());
                pdiBuilder.addTags(datasetIpId);
                sip.getProperties().setPdi(pdiBuilder.build());
                
                if (LOGGER.isDebugEnabled()) {
                    Gson gson = new Gson();
                    LOGGER.debug(gson.toJson(sip));
                }
                
            });
        }

        return sipCollection;
    }

    @Override
    public SIPCollection runPlugin(List<AcquisitionFile> acqFiles) throws ModuleException {
        String productName = acqFiles.get(0).getProduct().getProductName();

        LOGGER.info("Start SIP generation for product <{}>", productName);

        // TODO CMZ il faut avoir récupérer le processingChain auprès du microservice Ingest
        SIPCollectionBuilder sipCollectionBuilder = new SIPCollectionBuilder("processingChain", this.session);

        SIPBuilder sipBuilder = new SIPBuilder(productName);

        // Add all AcquisistionFile to the content information
        for (AcquisitionFile af : acqFiles) {
            try {
                sipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, af.getFile().toURI().toURL(),
                                                                        af.getChecksumAlgorithm(), af.getChecksum());
                sipBuilder.addContentInformation();
            } catch (MalformedURLException e) {
                LOGGER.error(e.getMessage(), e);
                throw new AcquisitionException(e.getMessage());
            }
        }

        // Add the meta-attributes
        SortedMap<Integer, Attribute> mm = this.createMetaDataPlugin(acqFiles);

        mm.forEach((k, v) -> {
            switch (v.getAttributeKey()) {
                case MISSION_ATTRIBUE:
                    sipBuilder.getPDIBuilder().addAdditionalProvenanceInformation(v.getAttributeKey(),
                                                                                  v.getValueList().get(0));
                    break;
                case INSTRUMENT_ATTRIBUE:
                    sipBuilder.getPDIBuilder().setInstrument((String) v.getValueList().get(0));
                    break;
                case CREATION_DATE_ATTRIBUE:
                    sipBuilder.getPDIBuilder().addProvenanceInformationEvent(CREATION_DATE_ATTRIBUE, "creation",
                                                                             (OffsetDateTime) v.getValueList().get(0));
                    break;
                case GEO_POINT_ATTRIBUTE:
                    sipBuilder.setGeometry((Point) v.getValueList().get(0));
                    break;
                default:
                    sipBuilder.getPDIBuilder().addContextInformation(v.getAttributeKey(), v.getValueList().get(0));
                    break;
            }
        });

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
