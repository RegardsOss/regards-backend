/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.gson.JsonObject;

import fr.cnes.regards.framework.geojson.GeoJsonFieldDescriptors;
import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.geojson.OaisFieldDescriptors;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
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

        SIP firstSIPwithGeometry = buildSipOne("SIP_001", "data1.fits").build();
        firstSIPwithGeometry
                .setGeometry(IGeometry.multiPoint(IGeometry.position(5.0, 5.0), IGeometry.position(25.0, 25.0)));

        List<Double> ld = new ArrayList<Double>();
        ld.add(19.0);
        ld.add(93.0);
        Double[] dd = ld.toArray(new Double[ld.size()]);
        firstSIPwithGeometry.setBbox(dd);

        collectionBuilder.add(firstSIPwithGeometry);

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());
        requestBuilderCustomizer.customizeHeaders().add(HttpHeaders.CONTENT_TYPE,
                                                        GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE);
        documentSipRequestBody(requestBuilderCustomizer);

        performDefaultPost(SIPController.TYPE_MAPPING, collectionBuilder.build(), requestBuilderCustomizer,
                           "SIP collection should be submitted.");
    }

    private void documentSipRequestBody(RequestBuilderCustomizer requestBuilderCustomizer) {
        ConstrainedFields fields = new ConstrainedFields(SIPCollection.class);

        List<FieldDescriptor> lfd = new ArrayList<FieldDescriptor>();
        lfd.add(fields.withPath("metadata.processing", "The ingest processing chain to used"));
        lfd.add(fields.withPath("metadata.session", "The ingestion session identifier"));
        lfd.add(fields.withPath("type", "Feature collection"));

        GeoJsonFieldDescriptors geoJsonDescriptors = new GeoJsonFieldDescriptors("features[].");
        lfd.addAll(geoJsonDescriptors.build());

        OaisFieldDescriptors oaisFiledDescriptors = new OaisFieldDescriptors("features[].");
        lfd.addAll(oaisFiledDescriptors.build());

        requestBuilderCustomizer.addDocumentationSnippet(PayloadDocumentation
                .relaxedRequestFields(Attributes.attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TITLE)
                        .value("Submission information package (SIP)")), lfd.toArray(new FieldDescriptor[lfd.size()])));
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_110")
    @Purpose("Get SIPs")
    public void getSips() {

        SIPCollectionBuilder collectionBuilder = new SIPCollectionBuilder(
                IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL, "sessionId");

        collectionBuilder.add(buildSipOne("SIP_001", "data1.fits").build());
        collectionBuilder.add(buildSipOne("SIP_002", "data2.fits").build());

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());
        requestBuilderCustomizer.customizeHeaders().add(HttpHeaders.CONTENT_TYPE,
                                                        GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE);

        performDefaultPost(SIPController.TYPE_MAPPING, collectionBuilder.build(), requestBuilderCustomizer,
                           "SIP collection should be submitted.");

        // Retrieve SIPs
        requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", Matchers.is(2)));

        documentSearchSipParameters(requestBuilderCustomizer);

        performDefaultGet(SIPController.TYPE_MAPPING, requestBuilderCustomizer, "Error retrieving SIPs");

    }

    private void documentSearchSipParameters(RequestBuilderCustomizer requestBuilderCustomizer) {
        List<ParameterDescriptor> paramDescrList = new ArrayList<ParameterDescriptor>();

        paramDescrList.add(RequestDocumentation.parameterWithName(SIPController.REQUEST_PARAM_SIP_ID).optional()
                .description("SIP identifier filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String")));
        paramDescrList.add(RequestDocumentation.parameterWithName(SIPController.REQUEST_PARAM_OWNER).optional()
                .description("SIP owner filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String")));
        paramDescrList.add(RequestDocumentation.parameterWithName(SIPController.REQUEST_PARAM_FROM).optional()
                .description("ISO Date time filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String")));
        paramDescrList.add(RequestDocumentation.parameterWithName(SIPController.REQUEST_PARAM_STATE).optional()
                .description("SIP state filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String")));
        paramDescrList.add(RequestDocumentation.parameterWithName(SIPController.REQUEST_PARAM_PROCESSING).optional()
                .description("Ingest processing name filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String")));
        paramDescrList.add(RequestDocumentation.parameterWithName(SIPController.REQUEST_PARAM_SESSION_ID).optional()
                .description("Session identifier filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String")));

        // Add request parameters documentation
        requestBuilderCustomizer.addDocumentationSnippet(RequestDocumentation.requestParameters(paramDescrList));
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_110")
    @Purpose("Get ingest session")
    public void getSession() {

        SIPCollectionBuilder collectionBuilder = new SIPCollectionBuilder(
                IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL, "session-id");

        collectionBuilder.add(buildSipOne("SIP_001", "data1.fits").build());
        collectionBuilder.add(buildSipTwo("SIP_002", "data2.fits").build());

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());
        requestBuilderCustomizer.customizeHeaders().add(HttpHeaders.CONTENT_TYPE,
                                                        GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE);
        performDefaultPost(SIPController.TYPE_MAPPING, collectionBuilder.build(), requestBuilderCustomizer,
                           "SIP collection should be submitted.");

        // Retrieve sessions
        requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", Matchers.is(1)));

        documentSearchSessionParameters(requestBuilderCustomizer);

        performDefaultGet(SIPSessionController.TYPE_MAPPING, requestBuilderCustomizer, "Error retrieving SIP sessions");
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_110")
    @Purpose("Get one ingest session")
    public void getOneSession() {
        String sessionId = "sessions-id";

        SIPCollectionBuilder collectionBuilder = new SIPCollectionBuilder(
                IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL, sessionId);

        collectionBuilder.add(buildSipOne("SIP_001", "data1.fits").build());
        collectionBuilder.add(buildSipTwo("SIP_002", "data2.fits").build());

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());
        requestBuilderCustomizer.customizeHeaders().add(HttpHeaders.CONTENT_TYPE,
                                                        GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE);
        performDefaultPost(SIPController.TYPE_MAPPING, collectionBuilder.build(), requestBuilderCustomizer,
                           "SIP collection should be submitted.");

        // Retrieve sessions
        requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());

        performDefaultGet(SIPSessionController.TYPE_MAPPING + SIPSessionController.ID_PATH, requestBuilderCustomizer,
                          "Error retrieving SIP sessions", sessionId);
    }

    private void documentSearchSessionParameters(RequestBuilderCustomizer requestBuilderCustomizer) {
        List<ParameterDescriptor> paramDescrList = new ArrayList<ParameterDescriptor>();

        paramDescrList.add(RequestDocumentation.parameterWithName(SIPSessionController.REQUEST_PARAM_ID).optional()
                .description("Ingestion's session identifier filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String")));
        paramDescrList.add(RequestDocumentation.parameterWithName(SIPSessionController.REQUEST_PARAM_FROM).optional()
                .description("ISO Date time starting filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String")));
        paramDescrList.add(RequestDocumentation.parameterWithName(SIPSessionController.REQUEST_PARAM_TO).optional()
                .description("ISO Date time ending filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String")));

        // Add request parameters documentation
        requestBuilderCustomizer.addDocumentationSnippet(RequestDocumentation.requestParameters(paramDescrList));
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
                             "http://www.iana.org/assignments/media-types/application/fits",
                             MediaType.valueOf("application/fits"));
        sipBuilder.addContentInformation();
        collectionBuilder.add(sipBuilder.build());

        // SIP 2
        sipBuilder = new SIPBuilder("SIP_002");
        sipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, Paths.get("data2.fits"),
                                                                "sdsdfm1211vsdfdsfd");
        sipBuilder.setSyntax("FITS(FlexibleImageTransport)",
                             "http://www.iana.org/assignments/media-types/application/fits",
                             MediaType.valueOf("application/fits"));
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
        
        documentFileRequestParameters(requestBuilderCustomizer);

        performDefaultFileUpload(SIPController.TYPE_MAPPING + SIPController.IMPORT_PATH, filePath,
                                 requestBuilderCustomizer, "Should be able to import valid SIP collection");
    }

    private void documentFileRequestParameters(RequestBuilderCustomizer requestBuilderCustomizer) {
        ParameterDescriptor paramFile = RequestDocumentation.parameterWithName(SIPController.REQUEST_PARAM_FILE)
                .optional().description("A file containing a SIP collection in GeoJson format")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String"));
        // Add request parameters documentation
        requestBuilderCustomizer.addDocumentationSnippet(RequestDocumentation.requestParameters(paramFile));
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_110")
    @Purpose("Ingest valid and invalid SIPs with multipart request")
    public void importPartialInvalidSips() {
        final Path filePath = Paths.get("src", "test", "resources", "invalidSipCollection.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isPartialContent());
        documentFileRequestParameters(requestBuilderCustomizer);

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
                             "http://www.iana.org/assignments/media-types/application/fits",
                             MediaType.valueOf("application/fits"));
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

    private SIPBuilder buildSipOne(String sipId, String fileName) {
        SIPBuilder sipBuilder = new SIPBuilder(sipId);
        sipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, Paths.get(fileName),
                                                                Paths.get(fileName).getFileName().toString(),
                                                                "b463726cfbb52d47e432bedf08edbec3", new Long(12345));
        sipBuilder.setSyntax("FITS(FlexibleImageTransport)",
                             "http://www.iana.org/assignments/media-types/application/fits",
                             MediaType.valueOf("application/fits"));
        sipBuilder.addContentInformation();
        sipBuilder.addDescriptiveInformation("longProperty", 987654);
        sipBuilder.addDescriptiveInformation("stringProperty", "Lorem ipsum dolor sit amet");
        sipBuilder.addDescriptiveInformation("dateProperty", OffsetDateTime
                .parse("2014-05-02T23:10:17-02:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        List<String> tags = new ArrayList<String>();
        tags.add("JASON_MISSION");
        sipBuilder.addTags(tags.toArray(new String[tags.size()]));

        sipBuilder.getPDIBuilder().addProvenanceInformationEvent("creation", "AIP creation", OffsetDateTime
                .parse("2014-01-02T23:10:05+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        sipBuilder.getPDIBuilder().addProvenanceInformationEvent("update", "instrument calibration", OffsetDateTime
                .parse("2014-01-09T09:01:37+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        sipBuilder.getPDIBuilder().addProvenanceInformationEvent("update", "data acquisition", OffsetDateTime
                .parse("2014-02-13T12:25:36+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        sipBuilder.getPDIBuilder()
                .addProvenanceInformationEvent("update", "new calibratiopn parameter 0.001", OffsetDateTime
                        .parse("2014-02-19T13:31:17+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        sipBuilder.getPDIBuilder().setFacility("CNES");
        sipBuilder.getPDIBuilder().setFilter("a filter");
        sipBuilder.getPDIBuilder().setDetector("detector");
        sipBuilder.getPDIBuilder().setInstrument("Doris instrument");
        sipBuilder.getPDIBuilder().setProposal("a proposal");
        sipBuilder.getPDIBuilder().addAdditionalProvenanceInformation("key-ref-1", "additional value 1");
        sipBuilder.getPDIBuilder().addAdditionalProvenanceInformation("key-ref-2", "additional value 2");
        sipBuilder.getPDIBuilder().addAdditionalProvenanceInformation("key-ref-3", "additional value 3");
        sipBuilder.getPDIBuilder().addReferenceInformation("ivoa", "ivo://XXXXX-YYYYYY");
        sipBuilder.getPDIBuilder().addReferenceInformation("doi", "https://doi.org/10.1007/s00223-003-0070-0");
        sipBuilder.getPDIBuilder().addReferenceInformation("ark",
                                                           "http://example.org/ark:/13030/654xz321/s3/f8.05v.tiff");
        sipBuilder.getPDIBuilder().addFixityInformation("key-fixity-1", "fixity value 1");
        sipBuilder.getPDIBuilder().addFixityInformation("key-fixity-2", "fixity value 2");
        sipBuilder.getPDIBuilder().setAccessRightInformation("licence", "access rights", OffsetDateTime
                .parse("2014-01-12T23:10:05+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        sipBuilder.addMiscInformation("key-misc-1", "misc value 1");
        sipBuilder.addMiscInformation("key-misc-2", OffsetDateTime.parse("2014-02-10T00:00:01.123Z",
                                                                         DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        sipBuilder
                .addMiscInformation("key-misc-3",
                                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");

        return sipBuilder;
    }

    private SIPBuilder buildSipTwo(String sipId, String fileName) {
        SIPBuilder sipBuilder = new SIPBuilder(sipId);
        sipBuilder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, Paths.get(fileName),
                                                                "3464e3f9a1dad119712c32e2290cbdf8");
        sipBuilder.setSyntax("FITS(FlexibleImageTransport)",
                             "http://www.iana.org/assignments/media-types/application/fits",
                             MediaType.valueOf("application/fits"));
        sipBuilder.addContentInformation();
        sipBuilder.addDescriptiveInformation("longProperty", "123456");
        sipBuilder
                .addDescriptiveInformation("stringProperty",
                                           "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium");
        sipBuilder.addDescriptiveInformation("dateProperty", "2018-09-09T10:00:00Z");
        JsonObject timePeriod = new JsonObject();
        timePeriod.addProperty("start_date", "2012-09-01T10:00:00Z");
        timePeriod.addProperty("stop_date", "2012-09-02T10:00:00Z");
        sipBuilder.addDescriptiveInformation("time_period", timePeriod);
        List<String> tags = new ArrayList<String>();
        tags.add("TAG_ONE");
        tags.add("OCEAN");
        tags.add("SPACE_OCEANOGRAPHY");
        sipBuilder.addTags(tags.toArray(new String[tags.size()]));
        sipBuilder.getPDIBuilder().setFacility("CS-SI");

        return sipBuilder;
    }
}
