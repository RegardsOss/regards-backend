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

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;

import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.acquisition.builder.MetaFileBuilder;
import fr.cnes.regards.modules.acquisition.builder.MetaProductBuilder;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IExecAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IScanDirectoryRepository;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;
import fr.cnes.regards.modules.acquisition.service.conf.AcquisitionServiceConfiguration;

/**
 * @author Christophe Mertz
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AcquisitionServiceConfiguration.class })
@ActiveProfiles({ "test", "disableDataProviderTask" })
@DirtiesContext
public class MetaProductServiceIT {

    /**
     * Static default tenant
     */
    @Value("${regards.tenant}")
    private String tenant;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IScanDirectoryRepository scanDirectoryRepository;

    @Autowired
    private IAcquisitionFileRepository acquisitionFileRepository;

    @Autowired
    private IMetaFileRepository metaFileRepository;

    @Autowired
    private IMetaProductService metaProductService;

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

    @Test
    public void createAndUpdateSimpleMetaProduct() throws ModuleException {
        Assert.assertEquals(0, metaProductRepository.count());
        String labelMetaProduct = "meta product label ";

        MetaProduct metaProduct1 = MetaProductBuilder.build(labelMetaProduct + "one").withCleanOriginalFile(true)
                .withIngestProcessingChain("ingest processing chain one").get();
        metaProductService.createOrUpdate(metaProduct1);

        Assert.assertEquals(1, metaProductRepository.count());
        Assert.assertEquals(metaProduct1, metaProductService
                .retrieveComplete(metaProductService.retrieve(labelMetaProduct + "one").getId()));

        MetaProduct metaProduct2 = MetaProductBuilder.build(labelMetaProduct + "two").withCleanOriginalFile(false)
                .withIngestProcessingChain("ingest processing chain two").get();
        metaProductService.createOrUpdate(metaProduct2);
        metaProductService.createOrUpdate(metaProduct2);

        Assert.assertEquals(2, metaProductRepository.count());
        Assert.assertEquals(metaProduct2, metaProductService
                .retrieveComplete(metaProductService.retrieve(labelMetaProduct + "two").getId()));

        metaProductService.delete(metaProduct1);
        Assert.assertEquals(1, metaProductRepository.count());

        metaProductService.delete(metaProduct2.getId());
        Assert.assertEquals(0, metaProductRepository.count());
    }

    @Test
    public void createAndUpdateCompleteMetaProduct() throws ModuleException {
        Assert.assertEquals(0, metaProductRepository.count());
        String labelMetaProduct = "meta product label ";
        String dirName = "/usr/local/sbin";
        Set<ScanDirectory> scanDirs = new HashSet<>();
        Set<MetaFile> metaFiles = new HashSet<>();

        // create 4 scan dir
        scanDirs.add(new ScanDirectory(dirName + "/one"));
        scanDirs.add(new ScanDirectory(dirName + "/two"));
        scanDirs.add(new ScanDirectory(dirName + "/three"));
        scanDirs.add(new ScanDirectory(dirName + "/for"));

        // create 3 Metafile
        metaFiles.add(MetaFileBuilder.build().isMandatory().withFilePattern("pattern1").withMediaType("file type")
                .withInvalidFolder("tmp/invalid1").get());
        MetaFile metaFile2 = MetaFileBuilder.build().isMandatory().withFilePattern("pattern2")
                .withMediaType("file type").withInvalidFolder("tmp/invalid2").withScanDirectories(scanDirs).get();
        metaFiles.add(metaFile2);
        metaFiles.add(MetaFileBuilder.build().isMandatory().withFilePattern("pattern3").withMediaType("file type")
                .withInvalidFolder("tmp/invalid3").get());

        // create a MetaProduct
        MetaProduct metaProduct1 = MetaProductBuilder.build(labelMetaProduct + "one").withCleanOriginalFile(true)
                .withIngestProcessingChain("ingest processing chain one").withMetaFiles(metaFiles).get();

        metaProductService.createOrUpdate(metaProduct1);

        Assert.assertEquals(1, metaProductRepository.count());
        Assert.assertEquals(3, metaFileRepository.count());
        Assert.assertEquals(4, scanDirectoryRepository.count());
        Assert.assertEquals(metaProduct1, metaProductService.retrieveComplete(metaProduct1.getId()));
        Assert.assertEquals(3, metaProduct1.getMetaFiles().size());

        // Add a scan dir to a MetaFile
        metaFiles.remove(metaFile2);
        metaFile2.addScanDirectory(new ScanDirectory(dirName + "/five"));
        metaFiles.add(metaFile2);

        metaProductService.createOrUpdate(metaProduct1);

        Assert.assertEquals(1, metaProductRepository.count());
        Assert.assertEquals(3, metaFileRepository.count());
        Assert.assertEquals(5, scanDirectoryRepository.count());
        Assert.assertEquals(metaProduct1, metaProductService.retrieveComplete(metaProduct1.getId()));
        Assert.assertEquals(3, metaProduct1.getMetaFiles().size());

        // Remove a MetaFile
        metaProduct1.getMetaFiles().remove(metaFile2);
        metaProductService.createOrUpdate(metaProduct1);

        Assert.assertEquals(1, metaProductRepository.count());
        Assert.assertEquals(2, metaFileRepository.count());
        Assert.assertEquals(0, scanDirectoryRepository.count());
        Assert.assertEquals(metaProduct1, metaProductService.retrieveComplete(metaProduct1.getId()));
        Assert.assertEquals(2, metaProduct1.getMetaFiles().size());

        metaProductService.delete(metaProduct1.getId());
        Assert.assertEquals(0, metaProductRepository.count());
    }

    @Test(expected = EntityNotFoundException.class)
    public void updateUnknownMetaProduct() throws ModuleException {
        Assert.assertEquals(0, metaProductRepository.count());
        String labelMetaProduct = "meta product label ";
        String dirName = "/usr/local/sbin";
        Set<ScanDirectory> scanDirs = new HashSet<>();
        Set<MetaFile> metaFiles = new HashSet<>();

        // create 1 scan dir
        scanDirs.add(new ScanDirectory(dirName + "/one"));

        // create 1 Metafiles
        metaFiles.add(MetaFileBuilder.build().isMandatory().withFilePattern("pattern1").withMediaType("file type")
                .withInvalidFolder("tmp/invalid1").get());

        // create 1 MetaProduct
        MetaProduct metaProduct1 = MetaProductBuilder.build(labelMetaProduct + "one").withCleanOriginalFile(true)
                .withIngestProcessingChain("ingest processing chain one").withMetaFiles(metaFiles).get();

        metaProductService.createOrUpdate(metaProduct1);

        Assert.assertEquals(1, metaProductRepository.count());

        metaProduct1.setId(9999L);
        metaProductService.update(9999L, metaProduct1);

        Assert.fail("error");
    }

    @Test(expected = EntityInconsistentIdentifierException.class)
    public void updateInconsistentMetaProduct() throws ModuleException {
        Assert.assertEquals(0, metaProductRepository.count());
        String labelMetaProduct = "meta product label ";
        String dirName = "/usr/local/sbin";
        Set<ScanDirectory> scanDirs = new HashSet<>();
        Set<MetaFile> metaFiles = new HashSet<>();

        // create 1 scan dir
        scanDirs.add(new ScanDirectory(dirName + "/one"));

        // create 1 Metafiles
        metaFiles.add(MetaFileBuilder.build().isMandatory().withFilePattern("pattern1").withMediaType("file type")
                .withInvalidFolder("tmp/invalid1").get());

        // create 1 MetaProduct
        MetaProduct metaProduct1 = MetaProductBuilder.build(labelMetaProduct + "one").withCleanOriginalFile(true)
                .withIngestProcessingChain("ingest processing chain one").withMetaFiles(metaFiles).get();

        metaProductService.createOrUpdate(metaProduct1);

        Assert.assertEquals(1, metaProductRepository.count());

        metaProductService.update(9999L, metaProduct1);

        Assert.fail("error");
    }

}
