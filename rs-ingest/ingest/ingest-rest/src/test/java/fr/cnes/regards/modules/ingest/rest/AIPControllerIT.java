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
package fr.cnes.regards.modules.ingest.rest;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.test.report.annotation.Requirements;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;
import fr.cnes.regards.modules.ingest.dto.request.SearchSelectionMode;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.aip.AIPStorageService;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;
import fr.cnes.regards.modules.test.IngestServiceIT;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link AIPEntity} REST API testing
 *
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=aip_controller_it",
                                   "regards.amqp.enabled=true",
                                   "regards.aips.save-metadata.bulk.delay=100",
                                   "regards.ingest.aip.delete.bulk.delay=100" })
@ContextConfiguration(classes = { AIPControllerIT.Config.class })
@ActiveProfiles(value = { "default", "test", "testAmqp", "StorageClientMock" }, inheritProfiles = false)
public class AIPControllerIT extends AbstractRegardsTransactionalIT {

    @Configuration
    @EnableScheduling
    static class Config {

        @Bean
        public DiscoveryClient discoveryClient() throws URISyntaxException {
            ServiceInstance service = Mockito.mock(ServiceInstance.class);
            Mockito.when(service.getUri()).thenReturn(new URI("http://localhost:7777"));

            DiscoveryClient client = Mockito.mock(DiscoveryClient.class);
            List<ServiceInstance> response = Lists.newArrayList();
            response.add(service);
            Mockito.when(client.getInstances(Mockito.anyString())).thenReturn(response);
            return client;

        }

    }

    @Autowired
    private StorageClientMock storageClient;

    @Autowired
    private ApplicationEventPublisher springPublisher;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IngestServiceIT ingestServiceTest;

    @Before
    public void init() throws Exception {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        storageClient.setBehavior(true, true);
        // Clean everything
        ingestServiceTest.init(getDefaultTenant());

        // resend the event of AppReady to reinit default data
        springPublisher.publishEvent(new ApplicationReadyEvent(Mockito.mock(SpringApplication.class),
                                                               null,
                                                               null,
                                                               null));
    }

    public void createAIP(String providerId,
                          Set<String> categories,
                          String sessionOwner,
                          String session,
                          String storage) {
        SIP sip = SIP.build(EntityType.DATA, providerId);
        sip.withDataObject(DataType.RAWDATA,
                           Paths.get("src", "main", "test", "resources", "data", "cdpp_collection.json"),
                           "MD5",
                           "azertyuiopqsdfmlmld");
        sip.withSyntax(MediaType.APPLICATION_JSON);
        sip.registerContentInformation();

        // Add creation event
        sip.withEvent(String.format("SIP %s generated", providerId));

        // Create event
        IngestMetadataDto mtd = IngestMetadataDto.build(sessionOwner,
                                                        session,
                                                        null,
                                                        IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                                        categories,
                                                        null,
                                                        StorageMetadata.build(storage));

        ingestServiceTest.sendIngestRequestEvent(sip, mtd);
    }

    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_AIP_110"),
                    @Requirement("REGARDS_DSL_STO_AIP_115"),
                    @Requirement("REGARDS_DSL_STO_AIP_120"),
                    @Requirement("REGARDS_DSL_STO_AIP_560") })
    @Purpose("Check that ingested AIPs are retrievable")
    public void searchAIPs() {

        // Create AIP
        createAIP("my object #1", Sets.newHashSet("CAT 1", "CAT 2"), "ESA", OffsetDateTime.now().toString(), "NAS #1");

        // Wait for ingestion finished
        ingestServiceTest.waitForIngestion(1, 10000, SIPState.STORED, getDefaultTenant());
        ingestServiceTest.waitAllRequestsFinished(10000, getDefaultTenant());

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        // Add request parameters documentation
        requestBuilderCustomizer.documentRequestBody(getSearchBodyDescriptors(""));
        // Add response body documentation
        requestBuilderCustomizer.documentResponseBody(documentResultingAIPEntity());

        SearchAIPsParameters body = new SearchAIPsParameters().withCategoriesIncluded(List.of("CAT 1"));

        performDefaultPost(AIPStorageService.AIPS_CONTROLLER_ROOT_PATH,
                           body,
                           requestBuilderCustomizer,
                           "Should retrieve AIPEntities");

        // Try a research with pagination and sort options
        performDefaultPost(AIPStorageService.AIPS_CONTROLLER_ROOT_PATH + "?page=0&size=20&sort=version,ASC",
                           body,
                           requestBuilderCustomizer,
                           "Should retrieve AIPEntities");
    }

    @Test
    @Requirements({ @Requirement("REGARDS_DSL_STO_AIP_150"),
                    @Requirement("REGARDS_DSL_STO_AIP_160"),
                    @Requirement("REGARDS_DSL_STO_AIP_050") })
    @Purpose("Retrieve AIPs thanks a providerId")
    public void testRetrieveAIPVersionHistory() {
        // Create AIP
        createAIP("testRetrieveAIPVersionHistory",
                  Sets.newHashSet("CAT 1", "CAT 2"),
                  "ESA",
                  OffsetDateTime.now().toString(),
                  "NAS #1");
        createAIP("testRetrieveAIPVersionHistory",
                  Sets.newHashSet("CAT 3", "CAT 4"),
                  "ESA",
                  OffsetDateTime.now().toString(),
                  "NAS #1");

        // Wait for ingestion finished
        ingestServiceTest.waitForIngestion(2, 10000, SIPState.STORED, getDefaultTenant());
        ingestServiceTest.waitAllRequestsFinished(10000, getDefaultTenant());
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.expectToHaveSize("$.content", 2);
        requestBuilderCustomizer.expectIsNotEmpty(
            "$.content[0].content.aip.properties.pdi.provenanceInformation.history");

        SearchAIPsParameters body = new SearchAIPsParameters().withProviderIdsIncluded(List.of(
            "testRetrieveAIPVersionHistory"));

        // Add request parameters documentation
        requestBuilderCustomizer.documentRequestBody(getSearchBodyDescriptors(""));
        // Add response body documentation
        requestBuilderCustomizer.documentResponseBody(documentResultingAIPEntity());

        performDefaultPost(AIPStorageService.AIPS_CONTROLLER_ROOT_PATH,
                           body,
                           requestBuilderCustomizer,
                           "Should retrieve AIPEntities");
    }

    @Test
    public void searchAIPTags() {

        // Create AIP
        String session = OffsetDateTime.now().toString();
        String sessionOwner = "ESA";
        createAIP("my object #1", Sets.newHashSet("CAT 1", "CAT 2"), sessionOwner, session, "NAS #1");

        // Wait for ingestion finished
        ingestServiceTest.waitForIngestion(1, 10000, SIPState.STORED, getDefaultTenant());
        ingestServiceTest.waitAllRequestsFinished(10000, getDefaultTenant());

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        // Add request parameters documentation
        requestBuilderCustomizer.documentRequestBody(getSearchBodyDescriptors(""));

        // Add response documentation
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(new ConstrainedFields(List.class).withPath("[]", "List of tags").type(JSON_ARRAY_TYPE));
        requestBuilderCustomizer.documentResponseBody(fields);

        SearchAIPsParameters body = new SearchAIPsParameters().withSessionOwner(sessionOwner).withSession(session);

        performDefaultPost(AIPStorageService.AIPS_CONTROLLER_ROOT_PATH + AIPController.TAG_SEARCH_PATH,
                           body,
                           requestBuilderCustomizer,
                           "Should retrieve AIP tags");
    }

    @Test
    public void searchAIPCategories() {

        // Create AIP
        String session = OffsetDateTime.now().toString();
        String sessionOwner = "ESA";
        createAIP("my object #1", Sets.newHashSet("CAT 1", "CAT 2"), sessionOwner, session, "NAS #1");

        // Wait for ingestion finished
        ingestServiceTest.waitForIngestion(1, 10000, SIPState.STORED, getDefaultTenant());
        ingestServiceTest.waitAllRequestsFinished(10000, getDefaultTenant());

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        // Add request parameters documentation
        requestBuilderCustomizer.documentRequestBody(getSearchBodyDescriptors(""));

        // Add response documentation
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(new ConstrainedFields(List.class).withPath("[]", "List of categories").type(JSON_ARRAY_TYPE));
        requestBuilderCustomizer.documentResponseBody(fields);

        SearchAIPsParameters body = new SearchAIPsParameters().withSessionOwner(sessionOwner).withSession(session);

        performDefaultPost(AIPStorageService.AIPS_CONTROLLER_ROOT_PATH + AIPController.CATEGORIES_SEARCH_PATH,
                           body,
                           requestBuilderCustomizer,
                           "Should retrieve AIP categories");
    }

    @Test
    public void searchAIPStorages() {

        // Create AIP
        String session = OffsetDateTime.now().toString();
        String sessionOwner = "ESA";
        createAIP("my object #1", Sets.newHashSet("CAT 1", "CAT 2"), sessionOwner, session, "NAS #1");

        // Wait for ingestion finished
        ingestServiceTest.waitForIngestion(1, 10000, SIPState.STORED, getDefaultTenant());
        ingestServiceTest.waitAllRequestsFinished(10000, getDefaultTenant());

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        // Add request parameters documentation
        requestBuilderCustomizer.documentRequestBody(getSearchBodyDescriptors(""));

        // Add response documentation
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(new ConstrainedFields(List.class).withPath("[]", "List of storages location (plugin business id)")
                                                    .type(JSON_ARRAY_TYPE));
        requestBuilderCustomizer.documentResponseBody(fields);

        SearchAIPsParameters body = new SearchAIPsParameters().withSessionOwner(sessionOwner).withSession(session);

        performDefaultPost(AIPStorageService.AIPS_CONTROLLER_ROOT_PATH + AIPController.STORAGE_SEARCH_PATH,
                           body,
                           requestBuilderCustomizer,
                           "Should retrieve AIP storages");
    }

    @Test
    public void testUpdateAips() {
        // Create AIP
        String session = OffsetDateTime.now().toString();
        String sessionOwner = "ESA";
        createAIP("my object #10", Sets.newHashSet("CAT 1", "CAT 2"), sessionOwner, session, "NAS #1");

        // Wait for ingestion finished
        ingestServiceTest.waitForIngestion(1, 10000, SIPState.STORED, getDefaultTenant());
        ingestServiceTest.waitAllRequestsFinished(10000, getDefaultTenant());

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        // Add request parameters documentation
        requestBuilderCustomizer.documentRequestBody(documentUpdateAIPRequestParameters());

        AIPUpdateParametersDto body = AIPUpdateParametersDto.build(new SearchAIPsParameters().withSessionOwner(
            sessionOwner).withSession(session), null, null, null, Lists.newArrayList("CAT 1"), null);

        performDefaultPost(AIPStorageService.AIPS_CONTROLLER_ROOT_PATH + AIPController.AIP_UPDATE_PATH,
                           body,
                           requestBuilderCustomizer,
                           "Should schedule AIP update");
    }

    @Test
    public void testDeleteAips() {
        // Given create AIP
        String category = "CAT 1";
        String session = OffsetDateTime.now().toString();
        String sessionOwner = "ESA";
        createAIP("my object #11", Sets.newHashSet(category, "CAT 2"), sessionOwner, session, "NAS #1");

        // Wait for ingestion finished
        ingestServiceTest.waitForIngestion(1, 10000, SIPState.STORED, getDefaultTenant());
        ingestServiceTest.waitAllRequestsFinished(10000, getDefaultTenant());

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        // Add request parameters documentation
        requestBuilderCustomizer.documentRequestBody(documentDeleteAIPRequestParameters());

        OAISDeletionPayloadDto body = OAISDeletionPayloadDto.build(SessionDeletionMode.IRREVOCABLY);
        body.withSessionOwner(sessionOwner).withSession(session).withCategoriesIncluded(List.of(category));

        // When
        performDefaultPost(AIPStorageService.AIPS_CONTROLLER_ROOT_PATH + AIPController.OAIS_DELETE_PATH,
                           body,
                           requestBuilderCustomizer,
                           "Should schedule AIP deletion");
    }

    @Test
    public void testDeleteAips_with_ValidationContraints() {
        // Given
        OAISDeletionPayloadDto body = new OAISDeletionPayloadDto();
        // When
        performDefaultPost(AIPStorageService.AIPS_CONTROLLER_ROOT_PATH + AIPController.OAIS_DELETE_PATH,
                           body,
                           customizer().expect(status().is(422)),
                           "Session deletion mode is required");

        // Given
        body.setDeletionMode(SessionDeletionMode.IRREVOCABLY);
        body.withAipIdsExcluded(IntStream.range(0, 1001).mapToObj(i -> "aipId" + i).toList());

        // When
        performDefaultPost(AIPStorageService.AIPS_CONTROLLER_ROOT_PATH + AIPController.OAIS_DELETE_PATH,
                           body,
                           customizer().expect(status().is(422)),
                           "List of values have a maximal size of 1000");
    }

    private List<FieldDescriptor> documentDeleteAIPRequestParameters() {
        List<FieldDescriptor> params = new ArrayList<>();
        ConstrainedFields constrainedFields = new ConstrainedFields(OAISDeletionPayloadDto.class);

        StringJoiner joiner = new StringJoiner(", ");
        for (SessionDeletionMode mode : SessionDeletionMode.values()) {
            joiner.add(mode.name());
        }
        params.add(constrainedFields.withPath("deletionMode", "deletionMode", "Type of deletion")
                                    .type(JSON_STRING_TYPE)
                                    .optional()
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(JSON_STRING_TYPE))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional. Allowed values : " + joiner)));

        params.addAll(getSearchBodyDescriptors(""));
        return params;
    }

    private List<FieldDescriptor> documentUpdateAIPRequestParameters() {
        List<FieldDescriptor> params = new ArrayList<>();
        ConstrainedFields constrainedFields = new ConstrainedFields(AIPUpdateParametersDto.class);

        String addTags = "addTags";
        params.add(constrainedFields.withPath(addTags, addTags, "A list of tags every entity will have")
                                    .optional()
                                    .type(List.class.getSimpleName())
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(List.class.getSimpleName()))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional.")));

        String removeTags = "removeTags";
        params.add(constrainedFields.withPath(removeTags, removeTags, "A list of tags every entity won't have anymore")
                                    .optional()
                                    .type(List.class.getSimpleName())
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(List.class.getSimpleName()))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional.")));

        String addCategories = "addCategories";
        params.add(constrainedFields.withPath(addCategories,
                                              addCategories,
                                              "A list of categories every entity will have")
                                    .optional()
                                    .type(List.class.getSimpleName())
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(List.class.getSimpleName()))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional.")));

        String removeCategories = "removeCategories";
        params.add(constrainedFields.withPath(removeCategories,
                                              removeCategories,
                                              "A list of categories every entity won't have anymore")
                                    .optional()
                                    .type(List.class.getSimpleName())
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(List.class.getSimpleName()))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional.")));

        String removeStorages = "removeStorages";
        params.add(constrainedFields.withPath(removeStorages,
                                              removeStorages,
                                              "A list of storages every entity won't use anymore")
                                    .optional()
                                    .type(List.class.getSimpleName())
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(List.class.getSimpleName()))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional.")));

        params.addAll(getSearchBodyDescriptors("criteria."));
        return params;
    }

    private List<FieldDescriptor> getSearchBodyDescriptors(String rootPath) {
        List<FieldDescriptor> params = new ArrayList<>();

        StringJoiner aipJoiner = new StringJoiner(", ");
        for (AIPState state : AIPState.values()) {
            aipJoiner.add(state.name());
        }

        StringJoiner selectionModeJoiner = new StringJoiner(", ");
        for (SearchSelectionMode mode : SearchSelectionMode.values()) {
            selectionModeJoiner.add(mode.name());
        }
        ConstrainedFields constrainedFields = new ConstrainedFields(SearchAIPsParameters.class);

        params.add(constrainedFields.withPath(rootPath + AIPController.REQUEST_PARAM_STATE,
                                              AIPController.REQUEST_PARAM_STATE,
                                              "AIP Entity state filter")
                                    .type(JSON_STRING_TYPE)
                                    .optional()
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(JSON_STRING_TYPE))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional. Multiple values allowed. Allowed values : "
                                                                 + aipJoiner)));

        params.add(constrainedFields.withPath(rootPath + AIPController.REQUEST_PARAM_FROM,
                                              AIPController.REQUEST_PARAM_FROM,
                                              "ISO Date time filtering on last update")
                                    .optional()
                                    .type(JSON_STRING_TYPE)
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(OffsetDateTime.class.getSimpleName()))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value(
                                                              "Optional. Required format : yyyy-MM-dd'T'HH:mm:ss.SSSZ")));

        params.add(constrainedFields.withPath(rootPath + AIPController.REQUEST_PARAM_TO,
                                              AIPController.REQUEST_PARAM_TO,
                                              "ISO Date time filtering on last update")
                                    .optional()
                                    .type(JSON_STRING_TYPE)
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(OffsetDateTime.class.getSimpleName()))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value(
                                                              "Optional. Required format : yyyy-MM-dd'T'HH:mm:ss.SSSZ")));

        params.add(constrainedFields.withPath(rootPath + AIPController.REQUEST_PARAM_TAGS,
                                              AIPController.REQUEST_PARAM_TAGS,
                                              "A list of tags every entity must have")
                                    .optional()
                                    .type(List.class.getSimpleName())
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(List.class.getSimpleName()))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional.")));

        params.add(constrainedFields.withPath(rootPath + AIPController.REQUEST_PARAM_PROVIDER_ID,
                                              AIPController.REQUEST_PARAM_PROVIDER_ID,
                                              "A list of provider ids filter")
                                    .optional()
                                    .type(JSON_STRING_TYPE)
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(JSON_STRING_TYPE))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value(
                                                              "Optional. If you add the % character, we will use the like operator to match provider id")));

        params.add(constrainedFields.withPath(rootPath + AIPController.REQUEST_PARAM_PROVIDER_ID,
                                              AIPController.REQUEST_PARAM_PROVIDER_ID,
                                              "A list of provider ids filter")
                                    .optional()
                                    .type(JSON_STRING_TYPE)
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(JSON_STRING_TYPE))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value(
                                                              "Optional. If you add the % character, we will use the like operator to match provider id")));

        params.add(constrainedFields.withPath(rootPath + AIPController.REQUEST_PARAM_SESSION_OWNER,
                                              AIPController.REQUEST_PARAM_SESSION_OWNER,
                                              "Session owner filter")
                                    .optional()
                                    .type(JSON_STRING_TYPE)
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(JSON_STRING_TYPE))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional.")));

        params.add(constrainedFields.withPath(rootPath + AIPController.REQUEST_PARAM_SESSION,
                                              AIPController.REQUEST_PARAM_SESSION,
                                              "Session filter")
                                    .optional()
                                    .type(JSON_STRING_TYPE)
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(JSON_STRING_TYPE))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional.")));

        params.add(constrainedFields.withPath(rootPath + AIPController.REQUEST_PARAM_CATEGORIES,
                                              AIPController.REQUEST_PARAM_CATEGORIES,
                                              "A list of categories every entity must have")
                                    .optional()
                                    .type(List.class.getSimpleName())
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(List.class.getSimpleName()))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional.")));

        params.add(constrainedFields.withPath(rootPath + AIPController.REQUEST_PARAM_STORAGES,
                                              AIPController.REQUEST_PARAM_STORAGES,
                                              "A list of storage names the entity must have, at least one")
                                    .optional()
                                    .type(List.class.getSimpleName())
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(List.class.getSimpleName()))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional.")));

        params.add(constrainedFields.withPath(rootPath + AIPController.REQUEST_PARAM_AIP_IDS,
                                              AIPController.REQUEST_PARAM_AIP_IDS,
                                              "A list of aip ids")
                                    .optional()
                                    .type(List.class.getSimpleName())
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(List.class.getSimpleName()))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional.")));

        params.add(constrainedFields.withPath(rootPath + "last", "last", "is it the last version?")
                                    .optional()
                                    .type(JSON_BOOLEAN_TYPE)
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional.")));

        params.add(constrainedFields.withPath(rootPath + AIPController.REQUEST_PARAM_STATE,
                                              AIPController.REQUEST_PARAM_STATE,
                                              "This attribute describe what the aipIds list mean.")
                                    .type(JSON_STRING_TYPE)
                                    .optional()
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                                                          .value(JSON_STRING_TYPE))
                                    .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                                          .value("Optional. Allowed values : " + selectionModeJoiner)));

        return params;
    }

    private List<FieldDescriptor> documentResultingAIPEntity() {

        ConstrainedFields constrainedFields = new ConstrainedFields(AIPEntity.class);
        List<FieldDescriptor> fields = new ArrayList<>();

        String prefix = "content[].content.";

        StringJoiner joiner = new StringJoiner(", ");
        for (AIPState state : AIPState.values()) {
            joiner.add(state.name());
        }
        fields.add(constrainedFields.withPath(prefix + "state", "state", "State", "Allowed values : " + joiner)
                                    .type(JSON_STRING_TYPE));

        fields.add(constrainedFields.withPath(prefix + "providerId", "providerId", "Provider id")
                                    .type(JSON_STRING_TYPE));

        fields.add(constrainedFields.withPath(prefix + "aipId", "aipId", "AIP id"));

        fields.add(constrainedFields.withPath(prefix + "creationDate", "creationDate", "Date of AIP creation"));

        fields.add(constrainedFields.withPath(prefix + "lastUpdate", "lastUpdate", "Date of last AIP update"));

        fields.add(constrainedFields.withPath(prefix + "tags", "tags", "List of tags").type(JSON_ARRAY_TYPE));

        fields.add(constrainedFields.withPath(prefix + "aip", "aip", "Generated AIP").type(JSON_OBJECT_TYPE));

        fields.add(constrainedFields.withPath(prefix + "sessionOwner", "sessionOwner", "Session owner")
                                    .type(JSON_STRING_TYPE));

        fields.add(constrainedFields.withPath(prefix + "session", "session", "Session").type(JSON_STRING_TYPE));

        fields.add(constrainedFields.withPath(prefix + "storages", "storages", "List of storage")
                                    .type(JSON_ARRAY_TYPE));

        fields.add(constrainedFields.withPath(prefix + "categories", "categories", "List of categories")
                                    .type(JSON_ARRAY_TYPE));

        return fields;
    }

    @After
    public void doAfter() {
        ingestServiceTest.init(getDefaultTenant());
    }
}
