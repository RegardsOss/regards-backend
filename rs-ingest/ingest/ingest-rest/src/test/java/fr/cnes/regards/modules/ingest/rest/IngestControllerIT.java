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
package fr.cnes.regards.modules.ingest.rest;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPCollectionBuilder;

/**
 *
 * Test SIP submission. Just test the REST layer with bean validation.
 *
 * @author Marc Sordi
 *
 */
@MultitenantTransactional
public class IngestControllerIT extends AbstractRegardsTransactionalIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestControllerIT.class);

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_110")
    @Purpose("Ingest SIP")
    public void ingestSips() {

        SIPCollectionBuilder collectionBuilder = new SIPCollectionBuilder("processingChain", "sessionId");

        // SIP 1
        SIPBuilder sipBuilder = new SIPBuilder("SIP_001");
        sipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, Paths.get("data1.fits"),
                                                                "sdsdfm1211vd");
        sipBuilder.addContentInformation();
        collectionBuilder.add(sipBuilder.build());

        // SIP 2
        sipBuilder = new SIPBuilder("SIP_002");
        sipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, Paths.get("data2.fits"),
                                                                "sdsdfm1211vsdfdsfd");
        sipBuilder.addContentInformation();
        collectionBuilder.add(sipBuilder.build());

        // Define expectations
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isCreated());
        // expectations.add(MockMvcResultMatchers.jsonPath(JSON_ID, Matchers.notNullValue()));

        performDefaultPostWithContentType(IngestController.TYPE_MAPPING, collectionBuilder.build(),
                                          GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE, expectations,
                                          "SIP collection should be submitted.");

    }
}
