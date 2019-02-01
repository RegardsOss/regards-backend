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
package fr.cnes.regards.modules.acquisition.rest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.restdocs.snippet.Attributes;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileInfoRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;

/**
 * {@link AcquisitionFile} REST API testing
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acquisition_it" })
public class AcquisitionFileControllerIT extends AbstractRegardsTransactionalIT {

    private Long chainId;

    @Autowired
    private IAcquisitionFileInfoRepository fileInfoRepository;

    @Autowired
    private IAcquisitionFileRepository acquisitionFileRepository;

    @Autowired
    private IAcquisitionProcessingChainRepository acquisitionProcessingChainRepository;

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Before
    public void init() throws ModuleException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        AcquisitionProcessingChain processingChain = AcquisitionTestUtils.getNewChain("Test");
        processingChain = processingService.createChain(processingChain);
        chainId = processingChain.getId();
        Path basePath = Paths.get("src", "test", "resources", "input");
        for (int i = 1; i < 3; i++) {
            Path file1 = basePath.resolve("data_" + i + ".txt");
            processingService.registerFile(file1, processingChain.getFileInfos().get(0), Optional.empty());
        }
    }

    @After
    public void cleanUp() throws ModuleException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        acquisitionFileRepository.deleteAll();
        fileInfoRepository.deleteAll();
        acquisitionProcessingChainRepository.deleteAll();
    }

    @Test
    public void searchAllFiles() throws ModuleException {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        documentRequestParameters(requestBuilderCustomizer);

        requestBuilderCustomizer.document(PayloadDocumentation.relaxedResponseFields(Attributes.attributes(Attributes
                                                                                                                   .key(RequestBuilderCustomizer.PARAM_TITLE)
                                                                                                                   .value("Acquisition file")),
                                                                                     documentAcquisitionFile()));

        performDefaultGet(AcquisitionFileController.TYPE_PATH, requestBuilderCustomizer, "Should retrieve files");
    }

    @Test
    public void searchFilesByState() throws ModuleException {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk().addParameter(
                AcquisitionFileController.REQUEST_PARAM_STATE,
                AcquisitionFileState.IN_PROGRESS.toString());
        documentRequestParameters(requestBuilderCustomizer);
        performDefaultGet(AcquisitionFileController.TYPE_PATH, requestBuilderCustomizer, "Should retrieve files");
    }

    private void documentRequestParameters(RequestBuilderCustomizer requestBuilderCustomizer) {

        ParameterDescriptor paramFilepath = RequestDocumentation
                .parameterWithName(AcquisitionFileController.REQUEST_PARAM_FILEPATH).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description("Entire file path filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"));

        StringJoiner joiner = new StringJoiner(", ");
        for (AcquisitionFileState state : AcquisitionFileState.values()) {
            joiner.add(state.name());
        }
        ParameterDescriptor paramState = RequestDocumentation
                .parameterWithName(AcquisitionFileController.REQUEST_PARAM_STATE).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description("Acquisition file state filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                    .value("Optional. Multiple values allowed. Allowed values : " + joiner.toString()));

        ParameterDescriptor paramProductId = RequestDocumentation
                .parameterWithName(AcquisitionFileController.REQUEST_PARAM_PRODUCT_ID).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_NUMBER_TYPE))
                .description("Product acquisition file(s) identifier filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"));

        ParameterDescriptor paramChainId = RequestDocumentation
                .parameterWithName(AcquisitionFileController.REQUEST_PARAM_CHAIN_ID).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_NUMBER_TYPE))
                .description("Acquisition chain identifier filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"));

        ParameterDescriptor paramFrom = RequestDocumentation
                .parameterWithName(AcquisitionFileController.REQUEST_PARAM_FROM).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description("ISO Date time filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                    .value("Optional. Required format : yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

        // Add request parameters documentation
        requestBuilderCustomizer.document(RequestDocumentation.requestParameters(paramFilepath,
                                                                                 paramState,
                                                                                 paramProductId,
                                                                                 paramChainId,
                                                                                 paramFrom));
    }

    private List<FieldDescriptor> documentAcquisitionFile() {

        ConstrainedFields constrainedFields = new ConstrainedFields(AcquisitionFile.class);
        List<FieldDescriptor> fields = new ArrayList<>();

        String prefix = "content[].content.";

        fields.add(constrainedFields.withPath(prefix + "filePath", "filePath", "Local file path"));

        StringJoiner joiner = new StringJoiner(", ");
        for (AcquisitionFileState mode : AcquisitionFileState.values()) {
            joiner.add(mode.name());
        }
        fields.add(constrainedFields
                           .withPath(prefix + "state", "state", "State", "Allowed values : " + joiner.toString()));

        fields.add(constrainedFields
                           .withPath(prefix + "error", "error", "Error details when acquisition file state is in ERROR")
                           .optional().type(JSON_STRING_TYPE));

        fields.add(constrainedFields.withPath(prefix + "acqDate", "acqDate", "ISO 8601 acquisition date"));

        fields.add(constrainedFields.withPath(prefix + "checksum", "checksum", "File checksum"));
        fields.add(constrainedFields.withPath(prefix + "checksumAlgorithm", "checksumAlgorithm", "Checksum algorithm"));

        return fields;
    }
}
