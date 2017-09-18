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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.modules.acquisition.dao.IChainGenerationRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
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
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ChaineGenerationServiceIT extends AbstractRegardsServiceTransactionalIT {

    //@MultitenantTransactional
    //public class ChaineGenerationServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChaineGenerationServiceIT.class);

    private static final String TENANT = "PROJECT";

    private static final String CHAINE_LABEL = "chaine label";

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IChainGenerationRepository chainRepository;

    @Autowired
    private IMetaProductRepository metaProductRepository;;

    @Autowired
    private IProductRepository productRepository;

    private IChainGenerationService chainfoService;

    private IMetaProductService metaProductService;

    private IProductService productService;

    @Before
    public void setUp() throws Exception {
        chainfoService = new ChaineGenerationService(chainRepository);
        metaProductService = new MetaProductService(metaProductRepository);
        productService = new ProductService(productRepository);

        tenantResolver.forceTenant(TENANT);
        
        cleanDb();
    }


//    public void topDown() {
//        cleanDb();
//
//    }

    @Test
    public void createChaine() {

        final String datasetName = "dataset name";

        // Create a first generation chain
        ChainGeneration chain = ChainGenerationBuilder.build(CHAINE_LABEL).isActive().withDataSet(datasetName).get();
        chain = chainfoService.save(chain);
        Assert.assertNotNull(chain);
        Assert.assertNotNull(chain.getId());

        // Create a meta product
        MetaProduct metaProduct = MetaProductBuilder.build("meta product").get();
        metaProduct = metaProductService.save(metaProduct);
        Assert.assertNotNull(metaProduct);
        Assert.assertNotNull(metaProduct.getId());

        // Set the MetaProduct to the ChainGeneration
        chain.setMetaProduct(metaProduct);
        chain = chainfoService.save(chain);
        Assert.assertNotNull(chain.getId());

        // Create a Product for the uniq MetaProduct
        Product aProduct = ProductBuilder.build("first product name").withStatus(ProductStatus.INIT.toString())
                .withMetaProduct(metaProduct).get();
        aProduct = productService.save(aProduct);
        Assert.assertNotNull(aProduct);

        // Link Product <-> MetaProduct
        metaProduct.addProduct(aProduct);
        metaProduct = metaProductService.save(metaProduct);
        Assert.assertNotNull(metaProduct);

        aProduct.setMetaProduct(metaProduct);
        aProduct = productService.save(aProduct);
        Assert.assertNotNull(aProduct);

        // Get the ChainGeneration
        List<ChainGeneration> chains = chainfoService.retrieveAll();
        Assert.assertNotNull(chains);
        Assert.assertEquals(1, chains.size());
        Assert.assertEquals(CHAINE_LABEL, chains.get(0).getLabel());
        Assert.assertEquals(datasetName, chains.get(0).getDataSet());
    }

    @Test
    public void deleteAProduct() {

        final String datasetName = "dataset name";

        // Create a first generation chain
        ChainGeneration chain = ChainGenerationBuilder.build(CHAINE_LABEL).isActive().withDataSet(datasetName).get();
        chain = chainfoService.save(chain);
        Assert.assertNotNull(chain);
        Assert.assertNotNull(chain.getId());

        // Create a meta product
        MetaProduct metaProduct = MetaProductBuilder.build("meta product").get();
        metaProduct = metaProductService.save(metaProduct);
        Assert.assertNotNull(metaProduct);
        Assert.assertNotNull(metaProduct.getId());

        // Set the MetaProduct to the ChainGeneration
        chain.setMetaProduct(metaProduct);
        chain = chainfoService.save(chain);
        Assert.assertNotNull(chain.getId());

        // Create a Product for the uniq MetaProduct
        Product aProduct = ProductBuilder.build("first product name").withStatus(ProductStatus.INIT.toString())
                .withMetaProduct(metaProduct).get();
        aProduct = productService.save(aProduct);
        Assert.assertNotNull(aProduct);
        Assert.assertNotNull(aProduct.getId());
        Assert.assertEquals(1, productService.retrieveAll().size());

        // Link Product <-> MetaProduct
        metaProduct.addProduct(aProduct);
        metaProduct = metaProductService.save(metaProduct);
        Assert.assertNotNull(metaProduct);

        aProduct.setMetaProduct(metaProduct);
        aProduct = productService.save(aProduct);
        Assert.assertNotNull(aProduct);
        Assert.assertNotNull(aProduct.getId());

        // Create a second Product for the uniq MetaProduct
        Product bProduct = ProductBuilder.build("second product name").withStatus(ProductStatus.INIT.toString())
                .withMetaProduct(metaProduct).get();
        bProduct = productService.save(bProduct);
        Assert.assertNotNull(bProduct);
        Assert.assertNotNull(bProduct.getId());
        Assert.assertEquals(2, productService.retrieveAll().size());

        // Links Product <-> MetaProduct
        metaProduct.addProduct(bProduct);
        metaProduct = metaProductService.save(metaProduct);
        Assert.assertNotNull(metaProduct);

        bProduct.setMetaProduct(metaProduct);
        bProduct = productService.save(bProduct);
        Assert.assertNotNull(bProduct);
        Assert.assertEquals(2, productService.retrieveAll().size());

        // Control the number of products from the MetaProduct
        Assert.assertEquals(2, metaProductService.retrieve(metaProduct.getId()).getProducts().size());

        // Delete a product
        productService.delete(aProduct.getId());
        Assert.assertEquals(1, productService.retrieveAll().size());

        // Get the MetaProduct
        Assert.assertEquals(1, metaProductService.retrieve(metaProduct.getId()).getProducts().size());
    }

    @After
    public void cleanDb() {
        productRepository.deleteAll();
        chainRepository.deleteAll();
        metaProductRepository.deleteAll();
    }
}
