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

import java.time.OffsetDateTime;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IScanDirectoryRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileBuilder;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductBuilder;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFileBuilder;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProductBuilder;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectoryBuilder;

/**
 * 
 * @author Christophe Mertz
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AcquisitionServiceConfiguration.class })
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class AcquisitionFileServiceIT extends AbstractRegardsServiceTransactionalIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionFileServiceIT.class);

    private static final String TENANT = "PROJECT";

    private static final String DATASET_NAME = "dataset name";

    private static final String META_PRODUCT_NAME = "meta product name";

    private static final String PRODUCT_NAME_1 = "first product name";

    private static final String PRODUCT_NAME_2 = "second product name";

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IMetaProductRepository metaProductRepository;;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IAcquisitionFileRepository acqfileRepository;

    @Autowired
    private IMetaFileRepository metafileRepository;

    @Autowired
    private IScanDirectoryRepository scandirRepository;

    private IMetaFileService metaFileService;

    private IMetaProductService metaProductService;

    private IProductService productService;

    private IAcquisitionFileService acqfileService;

    private IScanDirectoryService scandirService;;

    private MetaProduct metaProduct;

    private Product aProduct;

    private Product bProduct;

    @Before
    public void setUp() throws Exception {
        metaProductService = new MetaProductService(metaProductRepository);
        productService = new ProductService(productRepository);
        acqfileService = new AcquisitionFileService(acqfileRepository);
        metaFileService = new MetaFileService(metafileRepository);
        scandirService = new ScanDirectoryService(scandirRepository);

        tenantResolver.forceTenant(TENANT);

        cleanDb();

        initProduct();
    }

    private Product addProduct(MetaProduct metaProduct, String label) {
        Product product = productService.save(ProductBuilder.build(label).withStatus(ProductStatus.INIT.toString())
                .withMetaProduct(metaProduct).get());
        // Link Product <-> MetaProduct
        metaProduct.addProduct(product);
        metaProduct = metaProductService.save(metaProduct);
        product.setMetaProduct(metaProduct);
        return productService.save(product);
    }

    private void initProduct() {
        metaProduct = metaProductService.save(MetaProductBuilder.build(META_PRODUCT_NAME).get());
        aProduct = addProduct(metaProduct, PRODUCT_NAME_1);
        bProduct = addProduct(metaProduct, PRODUCT_NAME_2);
    }

    @Test
    public void testAcqFiles() {
        List<Product> products = productService.retrieveAll();
        Assert.assertEquals(2, products.size());

        // Create 3 ScanDirectory
        ScanDirectory scanDir1 = scandirService.save(ScanDirectoryBuilder.build("/var/regards/data/input")
                .withDateAcquisition(OffsetDateTime.now().minusDays(5)).get());
        ScanDirectory scanDir2 = scandirService.save(ScanDirectoryBuilder.build("/var/regards/data/input")
                .withDateAcquisition(OffsetDateTime.now().minusMinutes(15)).get());
        ScanDirectory scanDir3 = scandirService.save(ScanDirectoryBuilder.build("/var/regards/data/input")
                .withDateAcquisition(OffsetDateTime.now().minusSeconds(1358)).get());

        // Create a aMetaFile with the 3 ScanDirectory
        MetaFile aMetaFile = MetaFileBuilder.build("meta file un").withInvalidFolder("/var/regards/data/invalid")
                .withFileType(MediaType.APPLICATION_JSON_VALUE).withFilePattern("file pattern")
                .comment("test scan directory comment").isMandatory().addScanDirectory(scanDir1)
                .addScanDirectory(scanDir2).addScanDirectory(scanDir3).get();
        aMetaFile = metaFileService.save(aMetaFile);

        // Create 2 AcquisitionFile 
        AcquisitionFile acqFile1 = AcquisitionFileBuilder.build("file one")
                .withStatus(AcquisitionFileStatus.IN_PROGRESS.toString()).withSize(133L).get();
        acqFile1.setMetaFile(aMetaFile);
        acqFile1 = acqfileService.save(acqFile1);
        
        AcquisitionFile acqFile2 = AcquisitionFileBuilder.build("file two")
                .withStatus(AcquisitionFileStatus.IN_PROGRESS.toString()).withSize(15686L).get();
        acqFile2.setMetaFile(aMetaFile);
        acqFile2 = acqfileService.save(acqFile2);

        Assert.assertEquals(3, scandirService.retrieveAll().size());
        Assert.assertEquals(3, metaFileService.retrieve(aMetaFile.getId()).getScanDirectories().size());
        Assert.assertEquals(2, acqfileService.retrieveAll().size());
        
        // Remove a ScanDirectory
        aMetaFile.removeScanDirectory(scanDir2);
        metaFileService.save(aMetaFile);
        
        Assert.assertEquals(2, metaFileService.retrieve(aMetaFile.getId()).getScanDirectories().size());
        Assert.assertEquals(3, scandirService.retrieveAll().size());

        // Get a specific ScanDirectory in the MetaFile 
        ScanDirectory scanDirFound = aMetaFile.getScanDirectory(scanDir3.getId());
        Assert.assertNotNull(scanDirFound);
        Assert.assertEquals(scanDir3,scanDirFound);
    }

    @Test
    public void testMetaProduct() {
        List<MetaProduct> metaProducts = metaProductService.retrieveAll();
        Assert.assertEquals(1, metaProducts.size());
    }

    @After
    public void cleanDb() {
        productRepository.deleteAll();
        metaProductRepository.deleteAll();
        scandirRepository.deleteAll();
        acqfileRepository.deleteAll();
        metafileRepository.deleteAll();
    }
}
