/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.oais.dto.sip.SIPDtoBuilder;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.geojson.geometry.Point;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.utils.metamodel.MetaAttribute;
import fr.cnes.regards.framework.utils.model.*;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.plugins.ISipGenerationPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * A simple {@link Plugin} of type {@link ISipGenerationPlugin}.
 *
 * @author Christophe Mertz
 */
@Plugin(id = "TestGenerateSipPlugin",
        version = "1.0.0-SNAPSHOT",
        description = "TestGenerateSipPlugin",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class TestGenerateSipPlugin extends AbstractGenerateSIPPlugin implements ISipGenerationPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestGenerateSipPlugin.class);

    private static final String MISSION_ATTRIBUE = "Mission";

    private static final String INSTRUMENT_ATTRIBUE = "instrument";

    private static final String CREATION_DATE_ATTRIBUE = "creation date";

    private static final String GEO_POINT_ATTRIBUTE = "point coord";

    private static final Random random = new Random();

    @Override
    protected void addDataObjectsToSip(SIPDtoBuilder sipBuilder, Set<AcquisitionFile> acqFiles) throws ModuleException {
        for (AcquisitionFile af : acqFiles) {
            sipBuilder.getContentInformationBuilder()
                      .setDataObject(DataType.RAWDATA,
                                     af.getFilePath().toAbsolutePath(),
                                     AcquisitionProcessingChain.CHECKSUM_ALGORITHM,
                                     UUID.randomUUID().toString());
            sipBuilder.getContentInformationBuilder().setSyntax(af.getFileInfo().getMimeType());
            sipBuilder.addContentInformation();
        }
    }

    @Override
    public void addAttributesTopSip(SIPDtoBuilder sipBuilder, SortedMap<Integer, Attribute> mapAttrs)
        throws ModuleException {
        mapAttrs.forEach((k, v) -> {
            switch (v.getAttributeKey()) {
                case MISSION_ATTRIBUE:
                    sipBuilder.getPDIBuilder()
                              .addAdditionalProvenanceInformation(v.getAttributeKey(), v.getValueList().get(0));
                    break;
                case INSTRUMENT_ATTRIBUE:
                    sipBuilder.getPDIBuilder().setInstrument((String) v.getValueList().get(0));
                    break;
                case CREATION_DATE_ATTRIBUE:
                    sipBuilder.getPDIBuilder()
                              .addProvenanceInformationEvent(CREATION_DATE_ATTRIBUE,
                                                             "creation",
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
    }

    @Override
    public SortedMap<Integer, Attribute> createMetadataPlugin(Set<AcquisitionFile> acqFiles) throws ModuleException {
        int n = 0;
        String productName = acqFiles.iterator().next().getProduct().getProductName();

        LOGGER.info("Start create MetaData for the product <{}> ", productName);

        SortedMap<Integer, Attribute> attributeMap = new TreeMap<>();
        attributeMap.put(n++, createLongAttribute("orbit", 100));
        attributeMap.put(n++, createLongAttribute("order", 133));
        attributeMap.put(n++, createStringAttribute(MISSION_ATTRIBUE, "Viking"));
        attributeMap.put(n++, createStringAttribute(INSTRUMENT_ATTRIBUE, "instrument used for the measure"));
        attributeMap.put(n++, createStringAttribute("comment", "Hello Toulouse"));
        attributeMap.put(n++, createDateAttribute(CREATION_DATE_ATTRIBUE, OffsetDateTime.now()));
        attributeMap.put(n++, createDateAttribute("start date", OffsetDateTime.now().minusMinutes(45)));
        attributeMap.put(n++, createDateAttribute("stop  date", OffsetDateTime.now().minusMinutes(15)));
        attributeMap.put(n++,
                         createPointAttribute(GEO_POINT_ATTRIBUTE,
                                              -90 * random.nextDouble(),
                                              90 * random.nextDouble()));

        LOGGER.info("End  create MetaData for the product <{}> ", productName);

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
        IGeometry pp = IGeometry.point(IGeometry.position(longitude, latitude));
        GeoAttribute attr = new GeoAttribute();
        attr.setMetaAttribute(new MetaAttribute(name, AttributeTypeEnum.TYPE_GEO_LOCATION));
        attr.addValue(pp);
        return attr;
    }

}
