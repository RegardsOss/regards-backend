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
package fr.cnes.regards.modules.acquisition.rest;

import java.time.OffsetDateTime;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
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
@RegardsTransactional
public class ProductControllerTestIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IAcquisitionProcessingService acqService;

    @Autowired
    private IProductService productService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Before
    public void init() throws ModuleException {
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        // Init processing chain
        AcquisitionProcessingChain processingChain = AcquisitionTestUtils.getNewChain("laChaine");
        acqService.createChain(processingChain);

        // Create some products to search for
        Product product = new Product();
        product.setIpId("ipId");
        product.setLastUpdate(OffsetDateTime.now());
        product.setProductName("plop");
        product.setSession("session");
        product.setProcessingChain(processingChain);
        product.setSipState(ProductSIPState.NOT_SCHEDULED);
        product.setState(ProductState.ACQUIRING);
        productService.save(product);
    }

    @Test
    public void searchForProductsTest() throws ModuleException {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(ProductController.TYPE_PATH, requestBuilderCustomizer, "Should retrieve products");

        requestBuilderCustomizer.customizeRequestParam().param("sipState", "NOT_SCHEDULED", "QUEUED");
        performDefaultGet(ProductController.TYPE_PATH, requestBuilderCustomizer, "Should retrieve products");
    }

}
