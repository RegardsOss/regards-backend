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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPCollectionBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPEntityBuilder;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPSession;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.service.ISIPService;
import fr.cnes.regards.modules.ingest.service.ISIPSessionService;

/**
 *
 * Test SIP submission. Just test the REST layer with bean validation.
 *
 * @author Marc Sordi
 *
 */
@RegardsTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingest_it" })
public class SIPControllerIT extends AbstractRegardsTransactionalIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPControllerIT.class);

    @Autowired
    private ISIPService sipService;

    @Autowired
    private ISIPSessionService sessionService;

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_110")
    @Purpose("Ingest valid SIPs")
    public void ingestSips() {

        SIPCollectionBuilder collectionBuilder = new SIPCollectionBuilder(
                IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL, "sessionId");

        // SIP 1
        SIPBuilder sipBuilder = new SIPBuilder("SIP_001");
        sipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, Paths.get("data1.fits"),
                                                                "sdsdfm1211vd");
        sipBuilder.setSyntax("FITS(FlexibleImageTransport)",
                             "http://www.iana.org/assignments/media-types/application/fits", "application/fits");
        sipBuilder.addContentInformation();
        collectionBuilder.add(sipBuilder.build());

        // SIP 2
        sipBuilder = new SIPBuilder("SIP_002");
        sipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, Paths.get("data2.fits"),
                                                                "sdsdfm1211vsdfdsfd");
        sipBuilder.setSyntax("FITS(FlexibleImageTransport)",
                             "http://www.iana.org/assignments/media-types/application/fits", "application/fits");
        sipBuilder.addContentInformation();
        collectionBuilder.add(sipBuilder.build());

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());
        requestBuilderCustomizer.customizeHeaders().add(HttpHeaders.CONTENT_TYPE,
                                                        GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE);
        performDefaultPost(SIPController.TYPE_MAPPING, collectionBuilder.build(), requestBuilderCustomizer,
                           "SIP collection should be submitted.");

        requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", Matchers.is(2)));
        performDefaultGet(SIPController.TYPE_MAPPING, requestBuilderCustomizer, "Error retrieving SIPs");

        // Retrieve sessions
        requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", Matchers.is(1)));
        performDefaultGet(SIPSessionController.TYPE_MAPPING, requestBuilderCustomizer, "Error retrieving SIP sessions");
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_110")
    @Purpose("Ingest valid and invalid SIPs")
    public void ingestInvalidSips() {

        SIPCollectionBuilder collectionBuilder = new SIPCollectionBuilder(
                IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL, "sessionId");

        // SIP 1
        SIPBuilder sipBuilder = new SIPBuilder("SIP_001");
        sipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, Paths.get("data1.fits"), "FAKE_ALGO",
                                                                "sdsdfm1211vd");
        sipBuilder.setSyntax("FITS(FlexibleImageTransport)",
                             "http://www.iana.org/assignments/media-types/application/fits", "application/fits");
        sipBuilder.addContentInformation();
        collectionBuilder.add(sipBuilder.build());

        // SIP 2
        sipBuilder = new SIPBuilder("SIP_002");
        sipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, Paths.get("data2.fits"),
                                                                "sdsdfm1211vsdfdsfd");
        sipBuilder.setSyntax("FITS(FlexibleImageTransport)",
                             "http://www.iana.org/assignments/media-types/application/fits", "application/fits");
        sipBuilder.addContentInformation();
        collectionBuilder.add(sipBuilder.build());

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isPartialContent());
        requestBuilderCustomizer.customizeHeaders().add(HttpHeaders.CONTENT_TYPE,
                                                        GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE);
        performDefaultPost(SIPController.TYPE_MAPPING, collectionBuilder.build(), requestBuilderCustomizer,
                           "Partial valid collection should be submitted.");
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_110")
    @Purpose("Ingest valid SIPs with multipart request")
    public void importValidSips() {
        final Path filePath = Paths.get("src", "test", "resources", "sipCollection.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        performDefaultFileUpload(SIPController.TYPE_MAPPING + SIPController.IMPORT_PATH, filePath,
                                 requestBuilderCustomizer, "Should be able to import valid SIP collection");
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_110")
    @Purpose("Ingest valid and invalid SIPs with multipart request")
    public void importPartialInvalidSips() {
        final Path filePath = Paths.get("src", "test", "resources", "invalidSipCollection.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isPartialContent());

        performDefaultFileUpload(SIPController.TYPE_MAPPING + SIPController.IMPORT_PATH, filePath,
                                 requestBuilderCustomizer, "Should be able to import a partial valid SIP collection");
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_110")
    @Purpose("Ingest invalid SIPs with multipart request")
    public void importAllInvalidSips() {
        final Path filePath = Paths.get("src", "test", "resources", "allInvalidSipCollection.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isUnprocessableEntity());

        performDefaultFileUpload(SIPController.TYPE_MAPPING + SIPController.IMPORT_PATH, filePath,
                                 requestBuilderCustomizer, "Should be able to import a partial valid SIP collection");
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_310")
    @Purpose("Load SIP with validation errors")
    public void searchSipWithErrors() {

        // Create SIP
        SIPBuilder sipBuilder = new SIPBuilder("SIP_001");
        sipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, Paths.get("data1.fits"),
                                                                "sdsdfm1211vd");
        sipBuilder.setSyntax("FITS(FlexibleImageTransport)",
                             "http://www.iana.org/assignments/media-types/application/fits", "application/fits");
        sipBuilder.addContentInformation();
        SIP sip = sipBuilder.build();

        // Store SIP entity
        SIPSession session = sessionService.getSession("session", true);

        SIPEntity sipEntity = SIPEntityBuilder.build(DEFAULT_TENANT, session, sip,
                                                     IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL, "me", 1,
                                                     SIPState.INVALID, EntityType.DATA);
        sipEntity.setChecksum("12332323f2ds3d6g6df");
        sipEntity.setProcessingErrors(Arrays.asList("error1", "error2"));
        sipService.saveSIPEntity(sipEntity);

        // Get SIPS with search API
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(SIPController.TYPE_MAPPING, requestBuilderCustomizer, "Should found valid SIP");
    }
}
