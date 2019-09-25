/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.test.IngestServiceTest;

/**
* {@link AIPEntity} REST API testing
*
* @author Léo Mieulet
*
*/
@ActiveProfiles({ "testAmqp" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=aip_controller_it",
        "regards.amqp.enabled=true" })
@ContextConfiguration(classes = { AIPControllerIT.Config.class })
public class AIPControllerIT extends AbstractRegardsTransactionalIT {

    @Configuration
    @EnableScheduling
    static class Config {

    }

    @Autowired
    private ApplicationEventPublisher springPublisher;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IngestServiceTest ingestServiceTest;

    @Before
    public void init() throws Exception {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Clean everything
        ingestServiceTest.init();
        // resend the event of AppReady to reinit default data
        springPublisher.publishEvent(new ApplicationReadyEvent(Mockito.mock(SpringApplication.class), null, null));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    public void createAIP(String providerId, Set<String> categories, String sessionOwner, String session,
            String storage) {
        SIP sip = SIP.build(EntityType.DATA, providerId);
        sip.withDataObject(DataType.RAWDATA,
                           Paths.get("src", "main", "test", "resources", "data", "cdpp_collection.json"), "MD5",
                           "azertyuiopqsdfmlmld");
        sip.withSyntax(MediaType.APPLICATION_JSON_UTF8);
        sip.registerContentInformation();

        // Add creation event
        sip.withEvent(String.format("SIP %s generated", providerId));

        // Create event
        IngestMetadataDto mtd = IngestMetadataDto.build(sessionOwner, session,
                                                        IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL, categories,
                                                        StorageMetadata.build(storage));

        ingestServiceTest.sendIngestRequestEvent(sip, mtd);
    }

    @Test
    public void searchAIPs() {

        // Create AIP
        createAIP("my object #1", Sets.newHashSet("CAT 1", "CAT 2"), "ESA", OffsetDateTime.now().toString(), "NAS #1");

        // Wait for ingestion finished
        ingestServiceTest.waitForIngestion(1, 10000);

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.addParameter("categories", "CAT 1");
        documentRequestParameters(requestBuilderCustomizer);

        requestBuilderCustomizer.document(PayloadDocumentation.relaxedResponseFields(documentResultingAIPEntity()));

        performDefaultGet(AIPController.TYPE_MAPPING, requestBuilderCustomizer, "Should retrieve AIPEntity");
    }

    private void documentRequestParameters(RequestBuilderCustomizer requestBuilderCustomizer) {

        StringJoiner joiner = new StringJoiner(", ");
        for (AIPState state : AIPState.values()) {
            joiner.add(state.name());
        }

        ParameterDescriptor paramState = RequestDocumentation.parameterWithName(AIPController.REQUEST_PARAM_STATE)
                .optional().attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description("AIP Entity state filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                        .value("Optional. Multiple values allowed. Allowed values : " + joiner.toString()));

        ParameterDescriptor paramFrom = RequestDocumentation.parameterWithName(AIPController.REQUEST_PARAM_FROM)
                .optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                        .value(OffsetDateTime.class.getSimpleName()))
                .description("ISO Date time filtering on last update")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                        .value("Optional. Required format : yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

        ParameterDescriptor paramTo = RequestDocumentation.parameterWithName(AIPController.REQUEST_PARAM_TO).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE)
                        .value(OffsetDateTime.class.getSimpleName()))
                .description("ISO Date time filtering on last update")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                        .value("Optional. Required format : yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

        ParameterDescriptor paramTags = RequestDocumentation.parameterWithName(AIPController.REQUEST_PARAM_TAGS)
                .optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(List.class.getSimpleName()))
                .description("A list of tags every entity must have")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional."));

        ParameterDescriptor paramProviderId = RequestDocumentation
                .parameterWithName(AIPController.REQUEST_PARAM_PROVIDER_ID).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description("Provider id filter").attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                        .value("Optional. If you add the % character, we will use the like operator to match entities"));

        ParameterDescriptor paramSessionOwner = RequestDocumentation
                .parameterWithName(AIPController.REQUEST_PARAM_SESSION_OWNER).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description("Session owner filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional."));

        ParameterDescriptor paramSession = RequestDocumentation.parameterWithName(AIPController.REQUEST_PARAM_SESSION)
                .optional().attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description("Session filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional."));

        ParameterDescriptor paramCategories = RequestDocumentation
                .parameterWithName(AIPController.REQUEST_PARAM_CATEGORIES).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(List.class.getSimpleName()))
                .description("A list of categories every entity must have")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional."));

        ParameterDescriptor paramStorages = RequestDocumentation.parameterWithName(AIPController.REQUEST_PARAM_STORAGES)
                .optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(List.class.getSimpleName()))
                .description("A list of storage names the entity must have, at least one")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional."));

        // Add request parameters documentation
        requestBuilderCustomizer.document(RequestDocumentation
                .requestParameters(paramState, paramFrom, paramTo, paramTags, paramProviderId, paramSessionOwner,
                                   paramSession, paramCategories, paramStorages));
    }

    private List<FieldDescriptor> documentResultingAIPEntity() {

        ConstrainedFields constrainedFields = new ConstrainedFields(AIPEntity.class);
        List<FieldDescriptor> fields = new ArrayList<>();

        String prefix = "content[].content.";

        StringJoiner joiner = new StringJoiner(", ");
        for (AIPState state : AIPState.values()) {
            joiner.add(state.name());
        }
        fields.add(constrainedFields
                .withPath(prefix + "state", "state", "State", "Allowed values : " + joiner.toString())
                .type(JSON_STRING_TYPE));

        fields.add(constrainedFields.withPath(prefix + "providerId", "providerId", "Provider id")
                .type(JSON_STRING_TYPE));

        fields.add(constrainedFields.withPath(prefix + "aipId", "aipId", "AIP id"));

        fields.add(constrainedFields.withPath(prefix + "creationDate", "creationDate", "Date of AIP creation"));

        fields.add(constrainedFields.withPath(prefix + "lastUpdate", "lastUpdate", "Date of last AIP update"));

        fields.add(constrainedFields.withPath(prefix + "tags", "tags", "List of tags").type(JSON_ARRAY_TYPE));

        fields.add(constrainedFields.withPath(prefix + "sip", "sip", "Generated SIP").type(JSON_OBJECT_TYPE));

        fields.add(constrainedFields.withPath(prefix + "aip", "aip", "Generated AIP").type(JSON_OBJECT_TYPE));

        String prefixIngestMetadata = "content[].content.ingestMetadata.";

        fields.add(constrainedFields.withPath(prefixIngestMetadata + "sessionOwner", "sessionOwner", "Session owner")
                .type(JSON_STRING_TYPE));

        fields.add(constrainedFields.withPath(prefixIngestMetadata + "session", "session", "Session")
                .type(JSON_STRING_TYPE));

        fields.add(constrainedFields
                .withPath(prefixIngestMetadata + "ingestChain", "ingestChain", "Name of the ingest chain")
                .type(JSON_STRING_TYPE));

        fields.add(constrainedFields.withPath(prefixIngestMetadata + "storages", "storages", "List of storage")
                .type(JSON_ARRAY_TYPE));

        fields.add(constrainedFields.withPath(prefixIngestMetadata + "categories", "categories", "List of categories")
                .type(JSON_ARRAY_TYPE));

        String prefixStorages = "content[].content.ingestMetadata.storages[].";

        fields.add(constrainedFields
                .withPath(prefixStorages + "pluginBusinessId", "pluginBusinessId", "Destination storage identifier")
                .type(JSON_STRING_TYPE));

        fields.add(constrainedFields
                .withPath(prefixStorages + "storePath", "storePath",
                          "Optional path identifying the base directory in which to store related files")
                .type(JSON_STRING_TYPE).optional());

        fields.add(constrainedFields
                .withPath(prefixStorages + "targetTypes", "targetTypes",
                          "List of data object types accepted by this storage location (when storing AIPs)")
                .type(JSON_ARRAY_TYPE).optional());

        return fields;
    }

}
