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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.acquisition.builder.AcquisitionProcessingChainBuilder;
import fr.cnes.regards.modules.acquisition.builder.MetaProductBuilder;
import fr.cnes.regards.modules.acquisition.builder.ProductBuilder;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IExecAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IScanDirectoryRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain2;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.service.conf.AcquisitionServiceConfiguration;

/**
 * 
 * @author Christophe Mertz
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AcquisitionServiceConfiguration.class })
@ActiveProfiles({ "test", "disableDataProviderTask" })
@DirtiesContext
public class AcquisitionProcessingChainServiceIT {

    /**
     * Static default tenant
     */
    @Value("${regards.tenant}")
    private String tenant;

    private static final String CHAIN_LABEL = "the chain label";

    private static final String DATASET_NAME = "dataset name";

    private static final String META_PRODUCT_NAME = "meta product name";

    private static final String PRODUCT_NAME_1 = "first product name";

    private static final String PRODUCT_NAME_2 = "second product name";

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IAcquisitionProcessingChainService2 acqProcessChainService;

    @Autowired
    private IMetaProductService metaProductService;

    @Autowired
    private IProductService productService;

    @Autowired
    private IScanDirectoryRepository scanDirectoryRepository;

    @Autowired
    private IAcquisitionFileRepository acquisitionFileRepository;

    @Autowired
    private IMetaFileRepository metaFileRepository;

    @Autowired
    private IExecAcquisitionProcessingChainRepository execProcessingChainRepository;

    @Autowired
    private IAcquisitionProcessingChainRepository processingChainRepository;

    @Autowired
    private IMetaProductRepository metaProductRepository;

    @Autowired
    private IProductRepository productRepository;

    @BeforeTransaction
    protected void beforeTransaction() {
        tenantResolver.forceTenant(tenant);
    }

    @Before
    public void cleanDb() {
        execProcessingChainRepository.deleteAll();
        processingChainRepository.deleteAll();
        scanDirectoryRepository.deleteAll();
        acquisitionFileRepository.deleteAll();
        metaFileRepository.deleteAll();
        productRepository.deleteAll();
        metaProductRepository.deleteAll();
    }

    private Product addProduct(MetaProduct metaProduct, String productName) throws ModuleException {
        Product product = productService.save(ProductBuilder.build(productName).withStatus(ProductState.ACQUIRING)
                .withMetaProduct(metaProduct).get());
        metaProduct = metaProductService.createOrUpdate(metaProduct);
        product.setMetaProduct(metaProduct);
        return productService.save(product);
    }

    @Test
    public void createChaine() throws ModuleException {
        // Create a first generation chain
        AcquisitionProcessingChain2 chain = acqProcessChainService
                .createOrUpdate(AcquisitionProcessingChainBuilder.build(CHAIN_LABEL).isActive().withDataSet(DATASET_NAME).get());
        Assert.assertNotNull(chain);
        Assert.assertNotNull(chain.getId());
        Assert.assertEquals(acqProcessChainService.retrieveComplete(chain.getId()),chain);

        // Create a meta product
        MetaProduct metaProduct = metaProductService.createOrUpdate(MetaProductBuilder.build(META_PRODUCT_NAME).get());
        Assert.assertNotNull(metaProduct);
        Assert.assertNotNull(metaProduct.getId());

        // Set the MetaProduct to the AcquisitionProcessingChain
        chain.setMetaProduct(metaProduct);
        chain = acqProcessChainService.createOrUpdate(chain);
        Assert.assertNotNull(chain.getId());

        // Create a Product for the uniq MetaProduct
        Product aProduct = addProduct(metaProduct, PRODUCT_NAME_1);
        Assert.assertNotNull(aProduct);

        // Get the AcquisitionProcessingChain
        Page<AcquisitionProcessingChain2> chains = acqProcessChainService.retrieveAll(new PageRequest(0, 10));
        Assert.assertNotNull(chains.getContent());
        Assert.assertEquals(1, chains.getContent().size());
        Assert.assertEquals(CHAIN_LABEL, chains.getContent().get(0).getLabel());
        Assert.assertEquals(DATASET_NAME, chains.getContent().get(0).getDataSet());
    }

    @Test
    public void deleteAProduct() throws ModuleException {
        // Create a first generation chain
        AcquisitionProcessingChain2 chain = acqProcessChainService
                .createOrUpdate(AcquisitionProcessingChainBuilder.build(CHAIN_LABEL).isActive().withDataSet(DATASET_NAME).get());
        Assert.assertNotNull(chain);
        Assert.assertNotNull(chain.getId());

        // Create a meta product
        MetaProduct metaProduct = metaProductService.createOrUpdate(MetaProductBuilder.build(META_PRODUCT_NAME).get());
        Assert.assertNotNull(metaProduct);
        Assert.assertNotNull(metaProduct.getId());

        // Set the MetaProduct to the AcquisitionProcessingChain
        chain.setMetaProduct(metaProduct);
        chain = acqProcessChainService.createOrUpdate(chain);
        Assert.assertNotNull(chain.getId());

        // Create a Product for the uniq MetaProduct
        Product aProduct = addProduct(metaProduct, PRODUCT_NAME_1);
        Assert.assertEquals(1, productService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertNotNull(aProduct);
        Assert.assertNotNull(aProduct.getId());

        // Create a second Product for the uniq MetaProduct
        Product bProduct = addProduct(metaProduct, PRODUCT_NAME_2);
        Assert.assertNotNull(bProduct);
        Assert.assertNotNull(bProduct.getId());
        Assert.assertEquals(2, productService.retrieveAll(new PageRequest(0, 10)).getTotalElements());

        // Delete a product
        productService.delete(aProduct.getId());
        Assert.assertEquals(1, productService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
    }
}
