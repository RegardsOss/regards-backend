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
package fr.cnes.regards.modules.acquisition.rest;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;
import fr.cnes.regards.modules.acquisition.service.IProductService;

/**
 * Test for ProductController
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acquisition_it" })
public class ProductControllerTestIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IAcquisitionProcessingService acqService;

    @Autowired
    private IProductService productService;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IAcquisitionFileInfoRepository fileInfoRepository;

    @Autowired
    private IAcquisitionProcessingChainRepository acquisitionProcessingChainRepository;

    @Before
    public void init() throws ModuleException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Init processing chain
        AcquisitionProcessingChain processingChain = AcquisitionTestUtils.getNewChain("laChaine");
        acqService.createChain(processingChain);

        // Create some products to search for
        Product product = new Product();
        product.setIpId("ipId");
        product.setLastUpdate(OffsetDateTime.now());
        product.setProductName("ProductName");
        product.setSession("MySession");
        product.setProcessingChain(processingChain);
        product.setSipState(ProductSIPState.NOT_SCHEDULED);
        product.setState(ProductState.ACQUIRING);
        productService.save(product);
    }

    @After
    public void cleanUp() throws ModuleException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        fileInfoRepository.deleteAll();
        productRepository.deleteAll();
        acquisitionProcessingChainRepository.deleteAll();
    }

    @Test
    public void searchForProductsTest() throws ModuleException {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultGet(ProductController.TYPE_PATH, requestBuilderCustomizer, "Should retrieve products");
        documentRequestParameters(requestBuilderCustomizer);

        requestBuilderCustomizer.document(PayloadDocumentation.relaxedResponseFields(Attributes.attributes(Attributes
                                                                                                                   .key(RequestBuilderCustomizer.PARAM_TITLE)
                                                                                                                   .value("Product")),
                                                                                     documentProduct()));

        requestBuilderCustomizer.addParameter("sipState", "NOT_SCHEDULED", "QUEUED");
        performDefaultGet(ProductController.TYPE_PATH, requestBuilderCustomizer, "Should retrieve products");
    }

    private void documentRequestParameters(RequestBuilderCustomizer requestBuilderCustomizer) {

        StringJoiner joiner = new StringJoiner(", ");
        for (ProductState state : ProductState.values()) {
            joiner.add(state.name());
        }
        ParameterDescriptor paramState = RequestDocumentation.parameterWithName(ProductController.REQUEST_PARAM_STATE)
                .optional().attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description("Product state filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                    .value("Optional. Multiple values allowed. Allowed values : " + joiner.toString()));

        joiner = new StringJoiner(", ");
        for (ProductSIPState state : ProductSIPState.values()) {
            joiner.add(state.name());
        }
        ParameterDescriptor paramSipState = RequestDocumentation
                .parameterWithName(ProductController.REQUEST_PARAM_SIP_STATE).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description("Product SIP state filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                    .value("Optional. Multiple values allowed. Allowed values : " + joiner.toString()));

        ParameterDescriptor paramProductId = RequestDocumentation
                .parameterWithName(ProductController.REQUEST_PARAM_PRODUCT_NAME).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description("Product name filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"));

        ParameterDescriptor paramChainId = RequestDocumentation
                .parameterWithName(ProductController.REQUEST_PARAM_CHAIN_ID).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_NUMBER_TYPE))
                .description("Acquisition chain identifier filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"));

        ParameterDescriptor paramFrom = RequestDocumentation.parameterWithName(ProductController.REQUEST_PARAM_FROM)
                .optional().attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description("ISO Date time filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS)
                                    .value("Optional. Required format : yyyy-MM-dd'T'HH:mm:ss.SSSZ"));

        ParameterDescriptor paramSession = RequestDocumentation
                .parameterWithName(ProductController.REQUEST_PARAM_SESSION).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_STRING_TYPE))
                .description("Session name filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"));

        ParameterDescriptor noSession = RequestDocumentation
                .parameterWithName(ProductController.REQUEST_PARAM_NO_SESSION).optional()
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_TYPE).value(JSON_BOOLEAN_TYPE))
                .description("No session filter")
                .attributes(Attributes.key(RequestBuilderCustomizer.PARAM_CONSTRAINTS).value("Optional"));

        // Add request parameters documentation
        requestBuilderCustomizer.document(RequestDocumentation.requestParameters(paramState,
                                                                                 paramSipState,
                                                                                 paramProductId,
                                                                                 paramChainId,
                                                                                 paramFrom,
                                                                                 paramSession,
                                                                                 noSession));
    }

    private List<FieldDescriptor> documentProduct() {

        ConstrainedFields constrainedFields = new ConstrainedFields(Product.class);
        List<FieldDescriptor> fields = new ArrayList<>();

        String prefix = "content[].content.";

        StringJoiner joiner = new StringJoiner(", ");
        for (ProductState mode : ProductState.values()) {
            joiner.add(mode.name());
        }
        fields.add(constrainedFields
                           .withPath(prefix + "state", "state", "State", "Allowed values : " + joiner.toString()));

        joiner = new StringJoiner(", ");
        for (ProductSIPState mode : ProductSIPState.values()) {
            joiner.add(mode.name());
        }
        fields.add(constrainedFields.withPath(prefix + "sipState",
                                              "sipState",
                                              " SIP State",
                                              "Allowed values : " + joiner.toString()));

        fields.add(constrainedFields
                           .withPath(prefix + "error", "error", "Error details when product state is in error state")
                           .optional().type(JSON_STRING_TYPE));

        fields.add(constrainedFields.withPath(prefix + "lastUpdate", "lastUpdate", "ISO 8601 last product update"));
        fields.add(constrainedFields.withPath(prefix + "productName", "productName", "Product name"));
        fields.add(constrainedFields.withPath(prefix + "session", "session", "Session name").optional()
                           .type(JSON_STRING_TYPE));
//        fields.add(constrainedFields.withPath(prefix + "fileList[]", "fileList", "Acquired product files"));

        fields.add(constrainedFields.withPath(prefix + "sip", "sip", "Generated SIP").optional()
                           .type(JSON_OBJECT_TYPE));
        fields.add(constrainedFields.withPath(prefix + "ipId", "ipId", "SIP IP ID").optional().type(JSON_STRING_TYPE));

        return fields;
    }
}
