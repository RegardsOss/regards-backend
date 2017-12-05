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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.acquisition.builder.AcquisitionFileBuilder;
import fr.cnes.regards.modules.acquisition.builder.MetaFileBuilder;
import fr.cnes.regards.modules.acquisition.builder.MetaProductBuilder;
import fr.cnes.regards.modules.acquisition.builder.ProductBuilder;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IChainGenerationRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IProcessGenerationRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IScanDirectoryRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;
import fr.cnes.regards.modules.acquisition.service.conf.AcquisitionServiceConfiguration;

/**
 * 
 * @author Christophe Mertz
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AcquisitionServiceConfiguration.class })
@DirtiesContext
public class AcquisitionFileServiceIT {

    @Value("${regards.tenant}")
    private String tenant;

    private static final String META_PRODUCT_NAME = "meta product name";

    private static final String PRODUCT_NAME = "first product name";

    /*
     *  @see https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#MessageDigest
     */
    private static final String CHECKUM_ALGO = "SHA-256";

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IMetaFileService metaFileService;

    @Autowired
    private IMetaProductService metaProductService;

    @Autowired
    private IProductService productService;

    @Autowired
    private IAcquisitionFileService acqfileService;

    @Autowired
    private IScanDirectoryService scandirService;

    @Autowired
    private IScanDirectoryRepository scanDirectoryRepository;

    @Autowired
    private IAcquisitionFileRepository acquisitionFileRepository;

    @Autowired
    private IMetaFileRepository metaFileRepository;

    @Autowired
    private IProcessGenerationRepository processGenerationRepository;

    @Autowired
    private IChainGenerationRepository chainGenerationRepository;

    @Autowired
    private IMetaProductRepository metaProductRepository;

    @Autowired
    private IProductRepository productRepository;

    @BeforeTransaction
    public void beforeTransaction() {
        tenantResolver.forceTenant(tenant);
    }

    @Before
    public void cleanDb() {
        processGenerationRepository.deleteAll();
        chainGenerationRepository.deleteAll();
        scanDirectoryRepository.deleteAll();
        acquisitionFileRepository.deleteAll();
        metaFileRepository.deleteAll();
        metaProductRepository.deleteAll();
        productRepository.deleteAll();
    }

    private Product addProduct(MetaProduct metaProduct, String productName) {
        Product product = productService.save(ProductBuilder.build(productName).withStatus(ProductStatus.ACQUIRING)
                .withMetaProduct(metaProduct).get());
        // Link Product <-> MetaProduct
        metaProduct.addProduct(product);
        metaProduct = metaProductService.save(metaProduct);
        product.setMetaProduct(metaProduct);
        return productService.save(product);
    }

    @Test
    public void testAcqFiles() {
        Assert.assertEquals(0, scandirService.retrieveAll().size());

        // Create 3 ScanDirectory
        ScanDirectory scanDir1 = scandirService.save(new ScanDirectory("/var/regards/data/input01"));
        ScanDirectory scanDir2 = scandirService.save(new ScanDirectory("/var/regards/data/input02"));
        ScanDirectory scanDir3 = scandirService.save(new ScanDirectory("/var/regards/data/input03"));

        // Create a aMetaFile with the 3 ScanDirectory
        MetaFile aMetaFile = MetaFileBuilder.build().withInvalidFolder("/var/regards/data/invalid")
                .withMediaType(MediaType.APPLICATION_JSON_VALUE).withFilePattern("file pattern")
                .comment("test scan directory comment").isMandatory().addScanDirectory(scanDir1)
                .addScanDirectory(scanDir2).addScanDirectory(scanDir3).get();
        aMetaFile = metaFileService.save(aMetaFile);

        // Init Product and MetaProduct
        MetaProduct metaProduct = metaProductService.save(MetaProductBuilder.build(META_PRODUCT_NAME)
                .withChecksumAlgorithm(CHECKUM_ALGO).withCleanOriginalFile(Boolean.FALSE).addMetaFile(aMetaFile).get());
        Product aProduct = addProduct(metaProduct, PRODUCT_NAME);

        Assert.assertEquals(1, metaProductService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(metaProduct, metaProductService.retrieveComplete(metaProduct.getId()));

        // Create 2 AcquisitionFile 
        AcquisitionFile acqFile1 = AcquisitionFileBuilder.build("file one")
                .withStatus(AcquisitionFileStatus.IN_PROGRESS.toString()).withSize(133L)
                .withActivationDate(OffsetDateTime.now().minusDays(5)).withChecksum("XXXXXXXXXXXXXXX", CHECKUM_ALGO)
                .get();
        acqFile1.setMetaFile(aMetaFile);
        acqFile1.setProduct(aProduct);
        acqFile1 = acqfileService.save(acqFile1);

        // Add the AcquisitionFile to the Product
        aProduct.addAcquisitionFile(acqFile1);

        AcquisitionFile acqFile2 = AcquisitionFileBuilder.build("file two")
                .withStatus(AcquisitionFileStatus.IN_PROGRESS.toString()).withSize(15686L)
                .withChecksum("YYYYYYYYYYYYYYYYY", CHECKUM_ALGO).withActivationDate(OffsetDateTime.now()).get();
        acqFile2.setMetaFile(aMetaFile);
        acqFile2.setProduct(aProduct);
        acqFile2 = acqfileService.save(acqFile2);

        // Add the AcquisitionFile to the Product
        aProduct.addAcquisitionFile(acqFile2);

        Assert.assertEquals(3, scandirService.retrieveAll().size());
        Assert.assertEquals(3, metaFileService.retrieve(aMetaFile.getId()).getScanDirectories().size());
        Assert.assertEquals(2, acqfileService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(2, aProduct.getAcquisitionFile().size());
        Assert.assertEquals(aProduct,
                            acqfileService.retrieveAll(new PageRequest(0, 10)).getContent().get(0).getProduct());
        Assert.assertEquals(aProduct,
                            acqfileService.retrieveAll(new PageRequest(0, 10)).getContent().get(1).getProduct());
        Assert.assertEquals(2, acqfileService.findByStatus(AcquisitionFileStatus.IN_PROGRESS).size());
        Assert.assertEquals(2, acqfileService.findByStatusAndMetaFile(AcquisitionFileStatus.IN_PROGRESS, aMetaFile)
                .size());
        Assert.assertEquals(acqFile1, acqfileService.retrieve(acqFile1.getId()));

        // Remove a ScanDirectory
        aMetaFile.removeScanDirectory(scanDir2);
        metaFileService.save(aMetaFile);

        Assert.assertEquals(2, metaFileService.retrieve(aMetaFile.getId()).getScanDirectories().size());
        Assert.assertEquals(3, scandirService.retrieveAll().size());
        Assert.assertEquals(scanDir2, scandirService.retrieve(scanDir2.getId()));

        // Get a specific ScanDirectory in the MetaFile 
        ScanDirectory scanDirFound = aMetaFile.getScanDirectory(scanDir3.getId());
        Assert.assertNotNull(scanDirFound);
        Assert.assertEquals(scanDir3, scanDirFound);

        // Remove an AcquisitionFile to the Product
        aProduct.removeAcquisitionFile(acqFile1);
        Assert.assertEquals(1, aProduct.getAcquisitionFile().size());
    }

    @Test
    public void testAcqFilesWithoutProduct() {
        Assert.assertEquals(0, scandirService.retrieveAll().size());

        // Create 3 ScanDirectory
        ScanDirectory scanDir1 = scandirService.save(new ScanDirectory("/var/regards/data/input01"));
        ScanDirectory scanDir2 = scandirService.save(new ScanDirectory("/var/regards/data/input02"));
        ScanDirectory scanDir3 = scandirService.save(new ScanDirectory("/var/regards/data/input03"));

        // Create a aMetaFile with the 3 ScanDirectory
        MetaFile aMetaFile = MetaFileBuilder.build().withInvalidFolder("/var/regards/data/invalid")
                .withMediaType(MediaType.APPLICATION_JSON_VALUE).withFilePattern("file pattern")
                .comment("test scan directory comment").isMandatory().addScanDirectory(scanDir1)
                .addScanDirectory(scanDir2).addScanDirectory(scanDir3).get();
        aMetaFile = metaFileService.save(aMetaFile);

        // Create 2 AcquisitionFile 
        AcquisitionFile acqFile1 = AcquisitionFileBuilder.build("file one")
                .withStatus(AcquisitionFileStatus.IN_PROGRESS.toString()).withSize(133L)
                .withActivationDate(OffsetDateTime.now().minusDays(5)).withChecksum("XXXXXXXXXXXXXXX", CHECKUM_ALGO)
                .get();
        acqFile1.setMetaFile(aMetaFile);
        acqFile1 = acqfileService.save(acqFile1);

        AcquisitionFile acqFile2 = AcquisitionFileBuilder.build("file two")
                .withStatus(AcquisitionFileStatus.IN_PROGRESS.toString()).withSize(15686L)
                .withChecksum("YYYYYYYYYYYYYYYYY", CHECKUM_ALGO).withActivationDate(OffsetDateTime.now()).get();
        acqFile2.setMetaFile(aMetaFile);
        acqFile2 = acqfileService.save(acqFile2);

        Assert.assertEquals(3, scandirService.retrieveAll().size());
        Assert.assertEquals(3, metaFileService.retrieve(aMetaFile.getId()).getScanDirectories().size());
        Assert.assertEquals(2, acqfileService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(0, productService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
    }

    @Test
    public void getUnkonwScanDirectory() {
        // Init Product and MetaProduct
        MetaProduct metaProduct = metaProductService.save(MetaProductBuilder.build(META_PRODUCT_NAME)
                .withChecksumAlgorithm(CHECKUM_ALGO).withCleanOriginalFile(Boolean.TRUE).get());
        Product aProduct = addProduct(metaProduct, PRODUCT_NAME);

        // Create 3 ScanDirectory
        ScanDirectory scanDir1 = scandirService.save(new ScanDirectory("/var/regards/data/input"));
        ScanDirectory scanDir2 = scandirService.save(new ScanDirectory("/var/regards/data/input"));
        ScanDirectory scanDir3 = scandirService.save(new ScanDirectory("/var/regards/data/input"));

        // Create a MetaFile with the 2 ScanDirectory
        MetaFile aMetaFile = MetaFileBuilder.build().withInvalidFolder("/var/regards/data/invalid")
                .withMediaType(MediaType.APPLICATION_JSON_VALUE).withFilePattern("file pattern")
                .comment("test scan directory comment").isMandatory().addScanDirectory(scanDir1)
                .addScanDirectory(scanDir2).get();
        aMetaFile = metaFileService.save(aMetaFile);

        metaProduct.addMetaFile(aMetaFile);
        metaProductService.save(metaProduct);

        // Create 2 AcquisitionFile 
        AcquisitionFile acqFile1 = AcquisitionFileBuilder.build("file one")
                .withStatus(AcquisitionFileStatus.IN_PROGRESS.toString()).withSize(133L)
                .withChecksum("YYYYYYYYYYYYYYYYY", CHECKUM_ALGO).get();
        acqFile1.setMetaFile(aMetaFile);
        acqFile1 = acqfileService.save(acqFile1);

        // Add the AcquisitionFile to the Product
        aProduct.addAcquisitionFile(acqFile1);

        AcquisitionFile acqFile2 = AcquisitionFileBuilder.build("file two")
                .withStatus(AcquisitionFileStatus.IN_PROGRESS.toString())
                .withChecksum("YYYYYYYYYYYYYYYYY", CHECKUM_ALGO).withSize(15686L).get();
        acqFile2.setMetaFile(aMetaFile);
        acqFile2 = acqfileService.save(acqFile2);

        // Add the AcquisitionFile to the Product
        aProduct.addAcquisitionFile(acqFile2);

        // Get a specific ScanDirectory in the MetaFile 
        ScanDirectory scanDirFound = aMetaFile.getScanDirectory(scanDir3.getId());
        Assert.assertNull(scanDirFound);
    }

    @Test
    public void removeUnkonwScanDirectory() {
        // Create 3 ScanDirectory
        ScanDirectory scanDir1 = scandirService.save(new ScanDirectory("/var/regards/data/input"));
        ScanDirectory scanDir2 = scandirService.save(new ScanDirectory("/var/regards/data/input"));
        ScanDirectory scanDir3 = scandirService.save(new ScanDirectory("/var/regards/data/input"));

        // Create a MetaFile with the 2 ScanDirectory
        MetaFile aMetaFile = MetaFileBuilder.build().withInvalidFolder("/var/regards/data/invalid")
                .withMediaType(MediaType.APPLICATION_JSON_VALUE).withFilePattern("file pattern")
                .comment("test scan directory comment").isMandatory().addScanDirectory(scanDir1)
                .addScanDirectory(scanDir2).get();
        aMetaFile = metaFileService.save(aMetaFile);

        // Get an unknown ScanDirectory in the MetaFile 
        aMetaFile.removeScanDirectory(scanDir3);
        Assert.assertTrue(true);

        aMetaFile.removeScanDirectory(null);
        Assert.assertTrue(true);
    }

}
