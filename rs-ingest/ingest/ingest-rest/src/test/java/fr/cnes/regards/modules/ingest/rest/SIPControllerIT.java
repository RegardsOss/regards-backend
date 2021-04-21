/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.geojson.GeoJsonFieldDescriptors;
import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.geojson.OaisFieldDescriptors;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import fr.cnes.regards.modules.ingest.dto.sip.SearchSIPsParameters;

/**
 *
 * Test SIP submission. Just test the REST layer with bean validation.
 *
 * @author Marc Sordi
 *
 */
@RegardsTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingest_it",
        "regards.aips.save-metadata.bulk.delay=100", "regards.ingest.aip.delete.bulk.delay=100" })
@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)
public class SIPControllerIT extends AbstractRegardsTransactionalIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPControllerIT.class);

    private static final String SESSION_OWNER = "sessionOwner";

    private static final String SESSION = "session";

    private static final Set<String> CATEGORIES = Sets.newHashSet("CAT");

    private static final StorageMetadata STORAGE_METADATA = StorageMetadata.build("disk");

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_110")
    @Purpose("Ingest valid SIPs")
    public void ingestSips() {

        SIPCollection collection = SIPCollection
                .build(IngestMetadataDto.build(SESSION_OWNER, SESSION, IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                               CATEGORIES, STORAGE_METADATA));

        SIP firstSIPwithGeometry = buildSipOne("SIP_001", "data1.fits");
        firstSIPwithGeometry
                .setGeometry(IGeometry.multiPoint(IGeometry.position(5.0, 5.0), IGeometry.position(25.0, 25.0)));

        List<Double> ld = new ArrayList<Double>();
        ld.add(19.0);
        ld.add(93.0);
        Double[] dd = ld.toArray(new Double[ld.size()]);
        firstSIPwithGeometry.setBbox(dd);

        collection.add(firstSIPwithGeometry);

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        documentSipRequestBody(requestBuilderCustomizer);

        performDefaultPost(SIPController.TYPE_MAPPING, collection, requestBuilderCustomizer,
                           "SIP collection should be submitted.");
    }

    private void documentSipRequestBody(RequestBuilderCustomizer requestBuilderCustomizer) {
        ConstrainedFields fields = new ConstrainedFields(SIPCollection.class);

        List<FieldDescriptor> lfd = new ArrayList<FieldDescriptor>();
        lfd.add(fields.withPath("metadata.ingestChain", "The ingest processing chain to used"));
        lfd.add(fields.withPath("metadata.session", "The ingestion session name"));
        lfd.add(fields.withPath("metadata.sessionOwner", "The ingestion session source"));
        lfd.add(fields.withPath("metadata.storages", "Target storages"));
        lfd.add(fields.withPath("metadata.storages[].pluginBusinessId", "Storage identifier"));
        lfd.add(fields.withPath("metadata.storages[].targetTypes",
                                "List of data object types accepted by this storage location (when storing AIPs)"));

        lfd.add(fields.withPath("type", "Feature collection"));

        GeoJsonFieldDescriptors geoJsonDescriptors = new GeoJsonFieldDescriptors("features[].");
        lfd.addAll(geoJsonDescriptors.build());

        OaisFieldDescriptors oaisFiledDescriptors = new OaisFieldDescriptors("features[].");
        lfd.addAll(oaisFiledDescriptors.build());

        requestBuilderCustomizer.document(PayloadDocumentation
                .relaxedRequestFields(Attributes.attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TITLE)
                        .value("Submission information package (SIP)")), lfd.toArray(new FieldDescriptor[lfd.size()])));
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_110")
    @Purpose("Get SIPs")
    public void getSips() {

        SIPCollection collection = SIPCollection
                .build(IngestMetadataDto.build(SESSION_OWNER, SESSION, IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                               Sets.newHashSet("CAT"), STORAGE_METADATA));

        collection.add(buildSipOne("SIP_001", "data1.fits"));
        collection.add(buildSipOne("SIP_002", "data2.fits"));

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);

        performDefaultPost(SIPController.TYPE_MAPPING, collection, requestBuilderCustomizer,
                           "SIP collection should be submitted.");
        // End submission requests

        // No SIPs is already created / Just request
        requestBuilderCustomizer = customizer().expectStatusOk()
                .expect(MockMvcResultMatchers.jsonPath("$.metadata.totalElements", Matchers.is(0)));
        SearchSIPsParameters body = SearchSIPsParameters.build();
        documentSearchSipParameters(requestBuilderCustomizer);
        performDefaultPost(SIPController.TYPE_MAPPING, body, requestBuilderCustomizer, "Error retrieving SIPs");

    }

    private void documentSearchSipParameters(RequestBuilderCustomizer requestBuilderCustomizer) {
        List<ParameterDescriptor> paramDescrList = new ArrayList<ParameterDescriptor>();

        paramDescrList.add(RequestDocumentation.parameterWithName(SIPController.REQUEST_PARAM_PROVIDER_ID).optional()
                .description("SIP identifier filter")
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
        paramDescrList.add(RequestDocumentation.parameterWithName(SIPController.REQUEST_PARAM_SESSION_OWNER).optional()
                .description("Session source filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String")));
        paramDescrList.add(RequestDocumentation.parameterWithName(SIPController.REQUEST_PARAM_SESSION).optional()
                .description("Session name filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"))
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String")));

        // Add request parameters documentation
        requestBuilderCustomizer.document(RequestDocumentation.requestParameters(paramDescrList));
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_110")
    @Purpose("Ingest valid and invalid SIPs")
    public void ingestInvalidSips() {

        SIPCollection collection = SIPCollection
                .build(IngestMetadataDto.build(SESSION_OWNER, SESSION, IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                               Sets.newHashSet("CAT"), STORAGE_METADATA));

        // SIP 1
        SIP sip = SIP.build(EntityType.DATA, "SIP_001");
        sip.withDataObject(DataType.RAWDATA, Paths.get("data1.fits"), "FAKE_ALGO", "sdsdfm1211vd");
        sip.withSyntax("FITS(FlexibleImageTransport)", "http://www.iana.org/assignments/media-types/application/fits",
                       MediaType.valueOf("application/fits"));
        collection.add(sip.registerContentInformation());

        // SIP 2
        sip = SIP.build(EntityType.DATA, "SIP_002");
        sip.withDataObject(DataType.RAWDATA, Paths.get("data2.fits"), "sdsdfm1211vsdfdsfd");
        sip.withSyntax("FITS(FlexibleImageTransport)", "http://www.iana.org/assignments/media-types/application/fits",
                       MediaType.valueOf("application/fits"));
        collection.add(sip.registerContentInformation());

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk()
                .addHeader(HttpHeaders.CONTENT_TYPE, GeoJsonMediaType.APPLICATION_GEOJSON_VALUE);
        performDefaultPost(SIPController.TYPE_MAPPING, collection, requestBuilderCustomizer,
                           "Partial valid collection should be submitted.");
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_110")
    @Purpose("Ingest valid SIPs with multipart request")
    public void importValidSips() {
        final Path filePath = Paths.get("src", "test", "resources", "sipCollection.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();

        documentFileRequestParameters(requestBuilderCustomizer);

        performDefaultFileUpload(SIPController.TYPE_MAPPING + SIPController.IMPORT_PATH, filePath,
                                 requestBuilderCustomizer, "Should be able to import valid SIP collection");
    }

    private void documentFileRequestParameters(RequestBuilderCustomizer requestBuilderCustomizer) {
        ParameterDescriptor paramFile = RequestDocumentation.parameterWithName(SIPController.REQUEST_PARAM_FILE)
                .optional().description("A file containing a SIP collection in GeoJson format")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value("String"));
        // Add request parameters documentation
        requestBuilderCustomizer.document(RequestDocumentation.requestParameters(paramFile));
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_110")
    @Purpose("Ingest valid and invalid SIPs with multipart request")
    public void importPartialInvalidSips() {
        final Path filePath = Paths.get("src", "test", "resources", "invalidSipCollection.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
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
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();

        performDefaultFileUpload(SIPController.TYPE_MAPPING + SIPController.IMPORT_PATH, filePath,
                                 requestBuilderCustomizer, "Should be able to import a partial valid SIP collection");
    }

    private SIP buildSipOne(String providerId, String fileName) {

        SIP sip = SIP.build(EntityType.DATA, providerId);
        sip.withDataObject(DataType.RAWDATA, Paths.get(fileName), Paths.get(fileName).getFileName().toString(), "MD5",
                           "b463726cfbb52d47e432bedf08edbec3", new Long(12345));
        sip.withSyntax("FITS(FlexibleImageTransport)", "http://www.iana.org/assignments/media-types/application/fits",
                       MediaType.valueOf("application/fits"));
        sip.registerContentInformation();

        sip.withDescriptiveInformation("longProperty", 987654);
        sip.withDescriptiveInformation("stringProperty", "Lorem ipsum dolor sit amet");
        sip.withDescriptiveInformation("dateProperty", OffsetDateTime.parse("2014-05-02T23:10:17-02:00",
                                                                            DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        List<String> tags = new ArrayList<String>();
        tags.add("JASON_MISSION");
        sip.withContextTags(tags.toArray(new String[tags.size()]));

        sip.withProvenanceInformationEvent("creation", "AIP creation", OffsetDateTime
                .parse("2014-01-02T23:10:05+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        sip.withProvenanceInformationEvent("update", "instrument calibration", OffsetDateTime
                .parse("2014-01-09T09:01:37+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        sip.withProvenanceInformationEvent("update", "data acquisition", OffsetDateTime
                .parse("2014-02-13T12:25:36+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        sip.withProvenanceInformationEvent("update", "new calibratiopn parameter 0.001", OffsetDateTime
                .parse("2014-02-19T13:31:17+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        sip.withFacility("CNES");
        sip.withFilter("a filter");
        sip.withDetector("detector");
        sip.withInstrument("Doris instrument");
        sip.withProposal("a proposal");
        sip.withAdditionalProvenanceInformation("key-ref-1", "additional value 1");
        sip.withAdditionalProvenanceInformation("key-ref-2", "additional value 2");
        sip.withAdditionalProvenanceInformation("key-ref-3", "additional value 3");
        sip.withReferenceInformation("ivoa", "ivo://XXXXX-YYYYYY");
        sip.withReferenceInformation("doi", "https://doi.org/10.1007/s00223-003-0070-0");
        sip.withReferenceInformation("ark", "http://example.org/ark:/13030/654xz321/s3/f8.05v.tiff");
        sip.withFixityInformation("key-fixity-1", "fixity value 1");
        sip.withFixityInformation("key-fixity-2", "fixity value 2");
        sip.withAccessRightInformation("licence", "access rights", OffsetDateTime
                .parse("2014-01-12T23:10:05+01:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return sip;
    }

    //    @SuppressWarnings("unused")
    //    private SIP buildSipTwo(String providerId, String fileName) {
    //
    //        SIP sip = SIP.build(EntityType.DATA, providerId, Lists.newArrayList("CAT2"));
    //        sip.withDataObject(DataType.RAWDATA, Paths.get(fileName), "3464e3f9a1dad119712c32e2290cbdf8");
    //        sip.withSyntax("FITS(FlexibleImageTransport)", "http://www.iana.org/assignments/media-types/application/fits",
    //                       MediaType.valueOf("application/fits"));
    //        sip.registerContentInformation();
    //
    //        sip.withDescriptiveInformation("longProperty", "123456");
    //        sip.withDescriptiveInformation("stringProperty",
    //                                       "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium");
    //        sip.withDescriptiveInformation("dateProperty", "2018-09-09T10:00:00Z");
    //        JsonObject timePeriod = new JsonObject();
    //        timePeriod.addProperty("start_date", "2012-09-01T10:00:00Z");
    //        timePeriod.addProperty("stop_date", "2012-09-02T10:00:00Z");
    //        sip.withDescriptiveInformation("time_period", timePeriod);
    //        List<String> tags = new ArrayList<String>();
    //        tags.add("TAG_ONE");
    //        tags.add("OCEAN");
    //        tags.add("SPACE_OCEANOGRAPHY");
    //        sip.withContextTags(tags.toArray(new String[tags.size()]));
    //        sip.withFacility("CS-SI");
    //
    //        return sip;
    //    }
}
