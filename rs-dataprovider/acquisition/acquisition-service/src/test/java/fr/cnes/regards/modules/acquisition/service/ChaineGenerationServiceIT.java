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
package fr.cnes.regards.modules.acquisition.service;

import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.engine.spi.PersistenceContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.ChainGenerationBuilder;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductBuilder;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProductBuilder;

/**
 * 
 * @author Christophe Mertz
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AcquisitionServiceConfiguration.class })
@Transactional
public class ChaineGenerationServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChaineGenerationServiceIT.class);

    /**
     * Static default tenant
     */
    @Value("${regards.tenant}")
    private String tenant;

    private static final String CHAINE_LABEL = "chaine label";

    private static final String DATASET_NAME = "dataset name";

    private static final String META_PRODUCT_NAME = "meta product name";

    private static final String PRODUCT_NAME_1 = "first product name";

    private static final String PRODUCT_NAME_2 = "second product name";

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IChainGenerationService chainfoService;

    @Autowired
    private IMetaProductService metaProductService;

    @Autowired
    private IProductService productService;

    @BeforeTransaction
    protected void beforeTransaction() {
        tenantResolver.forceTenant(tenant);
    }

//    @Before
//    public void setUp() throws Exception {
//        tenantResolver.forceTenant(tenant);
//    }

    private Product addProduct(MetaProduct metaProduct, String productName) {
        Product product = productService.save(ProductBuilder.build(productName)
                .withStatus(ProductStatus.INIT.toString()).withMetaProduct(metaProduct).get());
        // Link Product <-> MetaProduct
        metaProduct.addProduct(product);
        metaProduct = metaProductService.save(metaProduct);
        product.setMetaProduct(metaProduct);
        return productService.save(product);
    }

    @Test
    public void createChaine() {
        // Create a first generation chain
        ChainGeneration chain = chainfoService
                .save(ChainGenerationBuilder.build(CHAINE_LABEL).isActive().withDataSet(DATASET_NAME).get());
        Assert.assertNotNull(chain);
        Assert.assertNotNull(chain.getId());

        // Create a meta product
        MetaProduct metaProduct = metaProductService.save(MetaProductBuilder.build(META_PRODUCT_NAME).get());
        Assert.assertNotNull(metaProduct);
        Assert.assertNotNull(metaProduct.getId());

        // Set the MetaProduct to the ChainGeneration
        chain.setMetaProduct(metaProduct);
        chain = chainfoService.save(chain);
        Assert.assertNotNull(chain.getId());

        // Create a Product for the uniq MetaProduct
        Product aProduct = addProduct(metaProduct, PRODUCT_NAME_1);
        Assert.assertNotNull(aProduct);

        // Get the ChainGeneration
        List<ChainGeneration> chains = chainfoService.retrieveAll();
        Assert.assertNotNull(chains);
        Assert.assertEquals(1, chains.size());
        Assert.assertEquals(CHAINE_LABEL, chains.get(0).getLabel());
        Assert.assertEquals(DATASET_NAME, chains.get(0).getDataSet());
    }

    @Test
    public void deleteAProduct() {
        // Create a first generation chain
        ChainGeneration chain = chainfoService
                .save(ChainGenerationBuilder.build(CHAINE_LABEL).isActive().withDataSet(DATASET_NAME).get());
        Assert.assertNotNull(chain);
        Assert.assertNotNull(chain.getId());

        // Create a meta product
        MetaProduct metaProduct = metaProductService.save(MetaProductBuilder.build(META_PRODUCT_NAME).get());
        Assert.assertNotNull(metaProduct);
        Assert.assertNotNull(metaProduct.getId());

        // Set the MetaProduct to the ChainGeneration
        chain.setMetaProduct(metaProduct);
        chain = chainfoService.save(chain);
        Assert.assertNotNull(chain.getId());

        // Create a Product for the uniq MetaProduct
        Product aProduct = addProduct(metaProduct, PRODUCT_NAME_1);
        Assert.assertEquals(1, productService.retrieveAll().size());
        Assert.assertNotNull(aProduct);
        Assert.assertNotNull(aProduct.getId());

        // Create a second Product for the uniq MetaProduct
        Product bProduct = addProduct(metaProduct, PRODUCT_NAME_2);
        Assert.assertNotNull(bProduct);
        Assert.assertNotNull(bProduct.getId());
        Assert.assertEquals(2, productService.retrieveAll().size());

        // Control the number of products from the MetaProduct
        Assert.assertEquals(2, metaProductService.retrieveComplete(metaProduct.getId()).getProducts().size());

        // Delete a product
        productService.delete(aProduct.getId());
        Assert.assertEquals(1, productService.retrieveAll().size());
    }
}
