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
package fr.cnes.regards.modules.acquisition.dao;

import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTest;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;

/**
 * Test complex queries
 * @author Marc Sordi
 *
 */
@Ignore("Development testing for complex queries")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.default_schema=jason2idgr")
public class ProductRepositoryTest extends AbstractDaoTest {

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Test
    public void test() {
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);

        List<Product> products = productRepository.findAll();
        Assert.assertNotNull(products);
        Assert.assertTrue(!products.isEmpty());

        Page<Product> productByState = productRepository
                .findByProcessingChainIngestChainAndSessionAndSipState("DefaultIngestChain", "NO_SESSION",
                                                                       ProductSIPState.SUBMISSION_SCHEDULED,
                                                                       new PageRequest(0, 10));
        Assert.assertNotNull(productByState);

    }
}
