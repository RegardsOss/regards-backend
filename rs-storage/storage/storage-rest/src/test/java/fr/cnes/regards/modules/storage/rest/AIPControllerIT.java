/*
 * Copyright 2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.rest;

import java.net.MalformedURLException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.job.AIPQueryFilters;
import fr.cnes.regards.modules.storage.domain.job.AddAIPTagsFilters;
import fr.cnes.regards.modules.storage.domain.job.DataStorageRemovalAIPFilters;
import fr.cnes.regards.modules.storage.domain.job.RemoveAIPTagsFilters;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@DirtiesContext
public class AIPControllerIT extends AbstractAIPControllerIT {

    private static final int MAX_WAIT = 60000;

    private static final String SESSION = "Session123";

    public static List<FieldDescriptor> documentQueryFilters() {

        ConstrainedFields constrainedFields = new ConstrainedFields(AIPQueryFilters.class);
        List<FieldDescriptor> descriptors = new ArrayList<>();

        descriptors.add(constrainedFields.withPath("state",
                                                   "state",
                                                   "State of the AIP",
                                                   "Available values: " + Arrays.stream(AIPState.values())
                                                           .map(Enum::name).collect(Collectors.joining(", ")))
                                .optional().type(String.class.getSimpleName()));

        descriptors.add(constrainedFields.withPath("session",
                                                   "session",
                                                   "Retrieves AIPs with a submission date before the provided value")
                                .optional().type(String.class.getSimpleName()));
        descriptors.add(constrainedFields.withPath("providerId", "providerId", "Provider id").optional()
                                .type(String.class.getSimpleName()));

        descriptors.add(constrainedFields.withPath("from",
                                                   "from",
                                                   "Retrieves AIPs with a submission date above the provided value")
                                .optional().type(OffsetDateTime.class.getSimpleName()));

        descriptors.add(constrainedFields
                                .withPath("to", "to", "Retrieves AIPs with a submission date before the provided value")
                                .optional().type(OffsetDateTime.class.getSimpleName()));

        descriptors.add(constrainedFields.withPath("aipIds", "aipIds", "A set of AIPs id (included)").optional()
                                .type(Set.class.getSimpleName()));

        descriptors.add(constrainedFields.withPath("aipIdsExcluded", "to", "A set of AIPs id excluded").optional()
                                .type(Set.class.getSimpleName()));

        descriptors.add(constrainedFields.withPath("tags", "tags", "List of tags that AIPs must have to be included")
                                .optional().type(List.class.getSimpleName()));

        descriptors.add(constrainedFields.withPath("storedOn",
                                                   "storedOn",
                                                   "Set of data storage id on which at least "
                                                           + "one file of the AIP has to be stored").optional()
                                .type(List.class.getSimpleName()));
        return descriptors;
    }

    public static List<FieldDescriptor> documentAddTagsQueryFilters() {
        List<FieldDescriptor> fieldDescriptors = AIPControllerIT.documentQueryFilters();

        ConstrainedFields constrainedFields = new ConstrainedFields(AddAIPTagsFilters.class);

        fieldDescriptors.add(constrainedFields.withPath("tagsToAdd", "tagsToAdd", "Tags to add to AIPs")
                                     .type(List.class.getSimpleName()));

        return fieldDescriptors;
    }

    public static List<FieldDescriptor> documentRemoveTagsQueryFilters() {
        List<FieldDescriptor> fieldDescriptors = AIPControllerIT.documentQueryFilters();

        ConstrainedFields constrainedFields = new ConstrainedFields(RemoveAIPTagsFilters.class);

        fieldDescriptors.add(constrainedFields.withPath("tagsToRemove", "tagsToRemove", "Tags to remove to AIPs")
                                     .type(List.class.getSimpleName()));

        return fieldDescriptors;
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_080")
    public void testStoreUnvalid() {
        aip.getProperties().getContentInformations().forEach(ci -> ci.getDataObject().setChecksum(null));
        // perform request
        performDefaultPost(AIPController.AIP_PATH,
                           new AIPCollection(aip),
                           customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY).addHeaders(getHeaders()),
                           "AIP storage should have been schedule properly");
    }

    @Test
    public void testStore() {
        // perform request
        performDefaultPost(AIPController.AIP_PATH,
                           new AIPCollection(aip),
                           customizer().expectStatusCreated().addHeaders(getHeaders()),
                           "AIP storage should have been schedule properly");
    }

    @Test
    public void testStoreTotalFail() {
        testStore();
        // now just try to store the same aip
        performDefaultPost(AIPController.AIP_PATH,
                           new AIPCollection(aip),
                           customizer().expectStatus(HttpStatus.UNPROCESSABLE_ENTITY).addHeaders(getHeaders()),
                           "Same AIP cannot be stored twice");
    }

    @Test
    public void testStoreFailPartial() throws MalformedURLException {
        testStore();
        // now just try to store the same aip
        AIP aip2 = getAIP();

        // perform request
        performDefaultPost(AIPController.AIP_PATH,
                           new AIPCollection(aip, aip2),
                           customizer().expectStatus(HttpStatus.PARTIAL_CONTENT).addHeaders(getHeaders()),
                           "Success should be partial, aip cannot be re stored but aip2 can be stored");
    }

    private Map<String, List<String>> getHeaders() {
        Map<String, List<String>> headers = Maps.newHashMap();
        headers.put(HttpConstants.ACCEPT,
                    Lists.newArrayList("application/json", MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.put(HttpConstants.CONTENT_TYPE, Lists.newArrayList(GeoJsonMediaType.APPLICATION_GEOJSON_VALUE));
        return headers;
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_140")
    public void testMakeAvailable() throws InterruptedException {
        // ask for an aip to be stored
        testStore();
        // wait for the AIP
        Thread.sleep(20000);
        // get the datafiles checksum
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Set<StorageDataFile> dataFiles = dataFileDao.findAllByAip(aip);
        Set<String> dataFilesChecksum = dataFiles.stream().map(StorageDataFile::getChecksum)
                .collect(Collectors.toSet());
        // ask for availability
        AvailabilityRequest availabilityRequest = new AvailabilityRequest(OffsetDateTime.now().plusDays(2),
                                                                          dataFilesChecksum
                                                                                  .toArray(new String[dataFilesChecksum
                                                                                          .size()]));
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk()
                .expect(MockMvcResultMatchers.content().json(gson.toJson(new AvailabilityResponse(Sets.newHashSet(),
                                                                                                  dataFiles,
                                                                                                  Sets.newHashSet()))));
        performDefaultPost(AIPController.AIP_PATH + AIPController.PREPARE_DATA_FILES,
                           availabilityRequest,
                           requestBuilderCustomizer,
                           "data should already be available as they are in an online data storage");
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_110")
    public void testRetrieveAip() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer
                .documentPathParameters(RequestDocumentation.parameterWithName(AIPController.AIP_ID_PATH_PARAM)
                                                .description("the AIP identifier (i.e. feature id)")
                                                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                    .value(String.class.getSimpleName()),
                                                            Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                    .value("Should respect UniformResourceName pattern")));
        performDefaultGet(AIPController.AIP_PATH + AIPController.AIP_ID_PATH,
                          requestBuilderCustomizer,
                          "we should have the aip",
                          aip.getId().toString());
    }

    @Test
    @Requirement("REGARDS_DSL_STOP_AIP_115")
    @Ignore
    public void testRetrieveDeletedAip() throws InterruptedException {
        testDelete();

        performDefaultGet(AIPController.AIP_PATH + AIPController.AIP_ID_PATH,
                          customizer().expectStatusOk().expect(MockMvcResultMatchers.jsonPath(
                                  "$.properties.pdi.provenanceInformation.history[*].type",
                                  IsCollectionContaining.hasItem(EventType.DELETION.name())))
                                  .expect(MockMvcResultMatchers.jsonPath(
                                          "$.properties.pdi.provenanceInformation.history[?(@.type == \""
                                                  + EventType.DELETION.name() + "\")].date").isNotEmpty()),
                          "we should have the aip",
                          aip.getId().toString());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_560")
    @Purpose("System allow to retrieve aips thanks to a list of ip id")
    public void testRetrieveAipBulk() {
        testStore();
        performDefaultPost(AIPController.AIP_PATH + AIPController.AIP_BULK,
                           Sets.newHashSet(aip.getId().toString()),
                           customizer().expectStatusOk(),
                           "we should have the aips");
    }

    @Test
    public void testRetrieveAIPWithDataStorages() throws InterruptedException {
        testStore();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        int wait = 0;
        // lets wait for this AIP to be stored
        while (aipDao.findOneByAipId(aip.getId().toString()).get().getState() != AIPState.STORED && wait < MAX_WAIT) {
            Thread.sleep(1000);
            wait += 1000;
        }
        Assert.assertTrue("AIP was not fully stored in time: " + wait, wait < MAX_WAIT);
        AIPQueryFilters filters = new AIPQueryFilters();
        filters.setSession(aip.getSession());
        filters.setFrom(aip.getSubmissionEvent().getDate());
        filters.setProviderId(aip.getProviderId());
        filters.setState(AIPState.STORED);
        filters.setTo(aip.getSubmissionEvent().getDate().plus(1, ChronoUnit.MINUTES));
        RequestBuilderCustomizer customizer = customizer().expectStatusOk().addParameter("page", "0")
                .addParameter("size", "20")
                .document(PayloadDocumentation.requestFields(AIPControllerIT.documentQueryFilters()));
        performDefaultPost(AIPController.AIP_PATH + AIPController.SEARCH_PATH,
                           filters,
                           customizer,
                           "we should have the aips");
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_560")
    @Purpose("System allow to retrieve aips thanks to a tag")
    public void testRetrieveAipTag() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.documentPathParameters(RequestDocumentation.parameterWithName("tag")
                                                                .description("the tag with which AIPs should be tagged")
                                                                .attributes(Attributes
                                                                                    .key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                                    .value(String.class
                                                                                                   .getSimpleName())),
                                                        RequestDocumentation
                                                                .parameterWithName(AIPController.AIP_ID_PATH_PARAM)
                                                                .description("the AIP identifier").attributes(Attributes
                                                                                                                      .key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                                                                      .value(String.class
                                                                                                                                     .getSimpleName()),
                                                                                                              Attributes
                                                                                                                      .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                                                                      .value("Should respect UniformResourceName pattern")));

        performDefaultGet(AIPController.AIP_PATH + AIPController.TAG,
                          requestBuilderCustomizer,
                          "we should have the aips",
                          aip.getId().toString(),
                          "tag");
    }

    @Test
    public void testDeleteFilesFromSingleDS() throws MalformedURLException, InterruptedException {
        testStore();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        int wait = 0;
        // lets wait for this AIP to be stored
        while (aipDao.findOneByAipId(aip.getId().toString()).get().getState() != AIPState.STORED && wait < MAX_WAIT) {
            Thread.sleep(1000);
            wait += 1000;
        }
        Assert.assertTrue("AIP was not fully stored in time: " + wait, wait < MAX_WAIT);
        DataStorageRemovalAIPFilters requestBody = new DataStorageRemovalAIPFilters();
        Long dataStorageId = pluginRepo.findOneByLabel(DATA_STORAGE_CONF_LABEL).getId();
        requestBody.getDataStorageIds().add(dataStorageId);
        requestBody.setState(AIPState.STORED);
        List<FieldDescriptor> paylodDoc = documentQueryFilters();
        paylodDoc.add(new ConstrainedFields(AIPQueryFilters.class).withPath("dataStorageIds",
                                                                            "dataStorageIds",
                                                                            "Data storages from which aip should be deleted")
                              .optional().type(Set.class.getSimpleName()));
        RequestBuilderCustomizer customizer = customizer().expect(MockMvcResultMatchers.status().isNoContent())
                .document(PayloadDocumentation.requestFields(paylodDoc));
        performDefaultPost(AIPController.AIP_PATH + AIPController.FILES_DELETE_PATH,
                           requestBody,
                           customizer,
                           "There should be any error on REST call as return value is" + " not conditional.");
    }

    @Test
    public void testDeleteAIPByQuery() throws MalformedURLException, InterruptedException {
        testStore();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        int wait = 0;
        // lets wait for this AIP to be stored
        while (aipDao.findOneByAipId(aip.getId().toString()).get().getState() != AIPState.STORED && wait < MAX_WAIT) {
            Thread.sleep(1000);
            wait += 1000;
        }
        Assert.assertTrue("AIP was not fully stored in time: " + wait, wait < MAX_WAIT);
        AIPQueryFilters requestParam = new AIPQueryFilters();
        RequestBuilderCustomizer customizer = customizer().expect(MockMvcResultMatchers.status().isNoContent())
                .document(PayloadDocumentation.requestFields(documentQueryFilters()));
        performDefaultPost(AIPController.AIP_PATH + AIPController.DELETE_SEARCH_PATH,
                           requestParam,
                           customizer,
                           "There should be any error on REST call as return value is not conditional.");
    }

    @Test
    public void testDelete() throws InterruptedException {
        testStore();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        int wait = 0;
        // lets wait for this AIP to be stored
        while (aipDao.findOneByAipId(aip.getId().toString()).get().getState() != AIPState.STORED && wait < MAX_WAIT) {
            Thread.sleep(1000);
            wait += 1000;
        }
        Assert.assertTrue("AIP was not fully stored in time: " + wait, wait < MAX_WAIT);
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusNoContent();
        requestBuilderCustomizer
                .documentPathParameters(RequestDocumentation.parameterWithName(AIPController.AIP_ID_PATH_PARAM)
                                                .description("the AIP identifier")
                                                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                    .value(String.class.getSimpleName()),
                                                            Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                    .value("Should respect UniformResourceName pattern")));
        performDefaultDelete(AIPController.AIP_PATH + AIPController.AIP_ID_PATH,
                             requestBuilderCustomizer,
                             "deletion of this aip should be possible",
                             aip.getId().toString());
    }

    @Test
    public void testBulkDelete() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusNoContent();

        Set<String> sipIpIds = new HashSet<>();
        sipIpIds.add(getRamdomSipId());
        sipIpIds.add(getRamdomSipId());
        performDefaultPost(AIPController.AIP_PATH + AIPController.AIP_BULK_DELETE,
                           sipIpIds,
                           requestBuilderCustomizer,
                           "AIPs should be deleted");
    }

    private String getRamdomSipId() {
        UniformResourceName sipId = new UniformResourceName(OAISIdentifier.SIP,
                                                            EntityType.COLLECTION,
                                                            getDefaultTenant(),
                                                            UUID.randomUUID(),
                                                            1);
        return sipId.toString();
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_120")
    public void testRetrieveAIPFiles() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer
                .documentPathParameters(RequestDocumentation.parameterWithName(AIPController.AIP_ID_PATH_PARAM)
                                                .description("the AIP identifier")
                                                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                    .value(String.class.getSimpleName()),
                                                            Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                    .value("Should respect UniformResourceName pattern")));
        performDefaultGet(AIPController.AIP_PATH + AIPController.OBJECT_LINK_PATH,
                          requestBuilderCustomizer,
                          "we should have the metadata of the files of the aip",
                          aip.getId().toString());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_150")
    public void testRetrieveAIPVersionHistory() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer
                .documentPathParameters(RequestDocumentation.parameterWithName(AIPController.AIP_ID_PATH_PARAM)
                                                .description("the AIP identifier")
                                                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                    .value(String.class.getSimpleName()),
                                                            Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                    .value("Should respect UniformResourceName pattern")));
        performDefaultGet(AIPController.AIP_PATH + AIPController.VERSION_PATH,
                          requestBuilderCustomizer,
                          "we should have the different versions of an aip",
                          aip.getId().toString());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_160")
    public void testRetrieveAIPHistory() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer
                .documentPathParameters(RequestDocumentation.parameterWithName(AIPController.AIP_ID_PATH_PARAM)
                                                .description("the AIP identifier")
                                                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                    .value(String.class.getSimpleName()),
                                                            Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                    .value("Should respect UniformResourceName pattern")));
        performDefaultGet(AIPController.AIP_PATH + AIPController.HISTORY_PATH,
                          requestBuilderCustomizer,
                          "we should have the history of an aip",
                          aip.getId().toString());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_ARC_200")
    @Requirement("REGARDS_DSL_STO_AIP_130")
    @Ignore
    public void testDownload() throws InterruptedException {
        // lets make files available
        // first lets make available the file
        testMakeAvailable();
        // lets ask for download now
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // lets get the datafile checksum
        Set<StorageDataFile> dataFiles = dataFileDao.findAllByAip(aip);
        StorageDataFile dataFile = dataFiles.toArray(new StorageDataFile[dataFiles.size()])[0];
        // now lets download it!
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().addHeaders(getHeaders())
                .addHeader(HttpConstants.ACCEPT, Lists.newArrayList(dataFile.getMimeType().toString()))
                .expectStatusOk();
        requestBuilderCustomizer
                .documentPathParameters(RequestDocumentation.parameterWithName(AIPController.AIP_ID_PATH_PARAM)
                                                .description("the AIP identifier")
                                                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                    .value(String.class.getSimpleName()),
                                                            Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                    .value("Should respect UniformResourceName pattern")),
                                        RequestDocumentation.parameterWithName("checksum")
                                                .description("the file to download checksum.")
                                                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                    .value(String.class.getSimpleName())));
        performDefaultGet(AIPController.AIP_PATH + AIPController.DOWNLOAD_AIP_FILE,
                          requestBuilderCustomizer,
                          "We should be downloading the data file",
                          aip.getId().toString(),
                          dataFile.getChecksum());
    }

    @Test
    public void testRetrieveAips() {
        testStore();
        RequestBuilderCustomizer customizer = customizer()
                .addParameter("from", OffsetDateTime.now().minusDays(40).toString())
                .addParameter("to", OffsetDateTime.now().toString()).addParameter("state", AIPState.VALID.toString())
                .addParameter("session", SESSION).addParameter("tags", "tag").expectStatusOk()
                .expectIsNotEmpty("$.content");

        customizer.documentRequestParameters(RequestDocumentation.parameterWithName("state")
                                                     .description("state the aips should be in")
                                                     .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                         .value(String.class.getSimpleName()),
                                                                 Attributes
                                                                         .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                         .value("Available values: " + Arrays
                                                                                 .stream(AIPState.values())
                                                                                 .map(Enum::name)
                                                                                 .reduce((first, second) -> first + ", "
                                                                                         + second).get())).optional(),
                                             RequestDocumentation.parameterWithName("from").description(
                                                     "date after which the aip should have been added to the system")
                                                     .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                         .value(String.class.getSimpleName()),
                                                                 Attributes
                                                                         .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                         .value("Should respect UTC format"))
                                                     .optional(),
                                             RequestDocumentation.parameterWithName("to").description(
                                                     "date before which the aip should have been added to the system")
                                                     .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                         .value(String.class.getSimpleName()),
                                                                 Attributes
                                                                         .key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                                         .value("Should respect UTC format"))
                                                     .optional(),
                                             RequestDocumentation.parameterWithName("session")
                                                     .description("search aips contained in the provided session")
                                                     .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                         .value(String.class.getSimpleName()))
                                                     .optional(),
                                             RequestDocumentation.parameterWithName("providerId")
                                                     .description("search aips having the provided provider id")
                                                     .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                         .value(String.class.getSimpleName()))
                                                     .optional(),
                                             RequestDocumentation.parameterWithName("tags")
                                                     .description("search aips tagged with one of provided tags")
                                                     .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                                         .value(List.class.getSimpleName()))
                                                     .optional());

        performDefaultGet(AIPController.AIP_PATH, customizer, "There should be some AIP to show");
    }

    @Test
    public void testRetrieveAllAipsTags() throws MalformedURLException {
        createSeveralAips();
        AIPQueryFilters requestParam = new AIPQueryFilters();
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk().expectToHaveSize("$", 10);
        performDefaultPost(AIPController.AIP_PATH + AIPController.TAG_SEARCH_PATH,
                           requestParam,
                           requestBuilderCustomizer,
                           "There should be some tags associated to AIPS");
    }

    @Test
    public void testRetrieveAipsTagsWithFilter() throws MalformedURLException {
        List<AIP> aips = createSeveralAips();

        AIPQueryFilters requestParam = new AIPQueryFilters();
        requestParam.setFrom(OffsetDateTime.now().minusDays(40));
        requestParam.setTo(OffsetDateTime.now());
        requestParam.setState(AIPState.STORAGE_ERROR);
        requestParam.setSession(SESSION);
        // Add STORAGE_ERROR AIPs
        requestParam.getAipIds().add(aips.get(0).getId().toString());
        requestParam.getAipIds().add(aips.get(1).getId().toString());
        // Exclude others
        requestParam.getAipIdsExcluded().add(aips.get(2).getId().toString());
        requestParam.getAipIdsExcluded().add(aips.get(3).getId().toString());
        requestParam.getAipIdsExcluded().add(aips.get(4).getId().toString());
        // Retrieve only entities having these tags
        requestParam.getTags().add("tag2");

        // There is only 4 different tags on STORAGE_ERROR AIPs
        int nbTags = 4;
        List<String> resultingTags = Arrays.asList("tag4", "tag2", "tag1", "tag3");
        RequestBuilderCustomizer requestBuilderCustomizer = customizer();
        requestBuilderCustomizer.document(PayloadDocumentation.requestFields(AIPControllerIT.documentQueryFilters()))
                .expectStatusOk().expectToHaveSize("$", nbTags)
                .expect(MockMvcResultMatchers.jsonPath("$", Matchers.containsInAnyOrder(resultingTags.toArray())));

        performDefaultPost(AIPController.AIP_PATH + AIPController.TAG_SEARCH_PATH,
                           requestParam,
                           requestBuilderCustomizer,
                           "There should be some tags associated to AIPS");
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_420")
    public void testAddTagToAIPsByQuery() throws MalformedURLException {
        List<AIP> aips = createSeveralAips();

        AddAIPTagsFilters requestParam = new AddAIPTagsFilters();
        requestParam.setFrom(OffsetDateTime.now().minusDays(40));
        requestParam.setTo(OffsetDateTime.now());
        requestParam.setState(AIPState.STORED);
        requestParam.setSession(SESSION);
        // Add STORED AIPs
        requestParam.getAipIds().add(aips.get(2).getId().toString());
        requestParam.getAipIds().add(aips.get(3).getId().toString());
        // Exclude others
        requestParam.getAipIdsExcluded().add(aips.get(0).getId().toString());
        requestParam.getAipIdsExcluded().add(aips.get(1).getId().toString());
        requestParam.getAipIdsExcluded().add(aips.get(4).getId().toString());
        // Add these tags to AIPs
        requestParam.getTagsToAdd().add("KGB");
        requestParam.getTagsToAdd().add("Mossad");
        requestParam.getTagsToAdd().add("DGSE");
        requestParam.getTagsToAdd().add("CIA");
        requestParam.getTagsToAdd().add("MI-6");
        requestParam.getTagsToAdd().add("FSB");

        RequestBuilderCustomizer requestBuilderCustomizer = customizer();
        requestBuilderCustomizer.document(PayloadDocumentation.requestFields(documentAddTagsQueryFilters()))
                .expectStatusOk();
        performDefaultPost(AIPController.AIP_PATH + AIPController.TAG_MANAGEMENT_PATH,
                           requestParam,
                           requestBuilderCustomizer,
                           "should set tags to AIPS");
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_430")
    public void testDeleteTagFromAIPsByQuery() throws MalformedURLException {
        List<AIP> aips = createSeveralAips();

        RemoveAIPTagsFilters requestParam = new RemoveAIPTagsFilters();
        requestParam.setFrom(OffsetDateTime.now().minusDays(40));
        requestParam.setTo(OffsetDateTime.now());
        requestParam.setState(AIPState.STORED);
        requestParam.setSession(SESSION);
        // Add STORED AIPs
        requestParam.getAipIds().add(aips.get(2).getId().toString());
        requestParam.getAipIds().add(aips.get(3).getId().toString());
        // Exclude others
        requestParam.getAipIdsExcluded().add(aips.get(0).getId().toString());
        requestParam.getAipIdsExcluded().add(aips.get(1).getId().toString());
        requestParam.getAipIdsExcluded().add(aips.get(4).getId().toString());
        // Add these tags to AIPs
        requestParam.getTagsToRemove().add("tag5353");
        requestParam.getTagsToRemove().add("tag8");
        requestParam.getTagsToRemove().add("tag5");

        RequestBuilderCustomizer requestBuilderCustomizer = customizer();

        requestBuilderCustomizer.document(PayloadDocumentation.requestFields(documentRemoveTagsQueryFilters()))
                .expectStatusOk();
        performDefaultPost(AIPController.AIP_PATH + AIPController.TAG_DELETION_PATH,
                           requestParam,
                           requestBuilderCustomizer,
                           "should remove tags to AIPS");
    }

    public List<AIP> createSeveralAips() throws MalformedURLException {
        aipSessionRepo.deleteAll();
        List<AIP> newAIPs = new ArrayList<>();
        AIPSession aipSession = new AIPSession();
        aipSession.setId(SESSION);
        aipSession.setLastActivationDate(OffsetDateTime.now());
        aipSession = aipSessionRepo.save(aipSession);

        // Create some AIP having errors
        AIP aipOnError1 = getNewAipWithTags(aipSession, "tag1", "tag2", "tag3");
        aipOnError1.setState(AIPState.STORAGE_ERROR);
        aipDao.save(aipOnError1, aipSession);
        newAIPs.add(aipOnError1);

        AIP aipOnError2 = getNewAipWithTags(aipSession, "tag4", "tag2");
        aipOnError2.setState(AIPState.STORAGE_ERROR);
        aipDao.save(aipOnError2, aipSession);
        newAIPs.add(aipOnError2);

        // Create some stored AIP
        AIP aipWaiting1 = getNewAipWithTags(aipSession, "tag5", "tag1", "tag123", "tag5353");
        aipWaiting1.setState(AIPState.STORED);
        aipDao.save(aipWaiting1, aipSession);
        newAIPs.add(aipWaiting1);

        AIP aipWaiting2 = getNewAipWithTags(aipSession, "tag7", "tag8");
        aipWaiting2.setState(AIPState.STORED);
        aipDao.save(aipWaiting2, aipSession);
        newAIPs.add(aipWaiting2);

        // Create some AIP on another session

        AIPSession aipSession2 = new AIPSession();
        aipSession2.setId("Test session 2");
        aipSession2.setLastActivationDate(OffsetDateTime.now());
        aipSession2 = aipSessionRepo.save(aipSession2);

        AIP aipDeleted11 = getNewAipWithTags(aipSession2, "tag6");
        aipDeleted11.setState(AIPState.DELETED);
        aipDao.save(aipDeleted11, aipSession2);
        newAIPs.add(aipDeleted11);
        return newAIPs;
    }

}
