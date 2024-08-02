/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.*;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=dataprovider_perf_tests" })
// locations = { "classpath:application-local.properties" })
//@Ignore("Performances tests")
public class PerformanceIT extends AbstractMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceIT.class);

    @Autowired
    private IAcquisitionProcessingService acqProService;

    @Autowired
    private IAcquisitionFileService fileService;

    @Autowired
    private IProductService productService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IJobService jobService;

    @Autowired
    private IJobInfoRepository jobRepo;

    private final Collection<AcquisitionProcessingChain> chains = Sets.newHashSet();

    private final int MAX_ITEM_PER_LOOP = 10_000;

    private final int MAX_LOOPS = 10;

    private final String sessionName = "SESSION PERF";

    public static boolean initialized = false;

    @Before
    public void init() throws ModuleException {
        tenantResolver.forceTenant(getDefaultTenant());
        jobRepo.deleteAll();
        if (!initialized) {
            clear();
            populate();
            initialized = true;
        }
    }

    private void clear() throws ModuleException {
        acqProService.getFullChains(PageRequest.of(0, 1000)).getContent().forEach(c -> {
            try {
                c.setActive(false);
                acqProService.updateChain(c);
                acqProService.deleteChain(c.getId());
            } catch (ModuleException e) {
                e.printStackTrace();
            }
        });
    }

    private void populate() throws ModuleException {
        long startTime = System.currentTimeMillis();

        // populate bdd
        int count = 0;
        for (int k = 0; k < MAX_LOOPS; k++) {
            Collection<AcquisitionFile> files = Sets.newHashSet();
            AcquisitionProcessingChain chain = acqProService.createChain(create("chaine first"));
            chains.add(chain);
            for (int i = 0; i < MAX_ITEM_PER_LOOP; i++) {
                AcquisitionFile file = new AcquisitionFile();
                file.setAcqDate(OffsetDateTime.now());
                file.setFileInfo(chain.getFileInfos().stream().findAny().get());
                file.setFilePath(Paths.get("/dir/file_" + k + "_" + i));
                file.setState(AcquisitionFileState.ACQUIRED);
                files.add(file);
            }
            files = fileService.save(files);

            Collection<Product> products = Sets.newHashSet();
            for (AcquisitionFile file : files) {
                Product product = new Product();
                product.setProductName(file.getFilePath().getFileName().toString());
                product.setProcessingChain(chain);
                product.setSession(sessionName);
                product.addAcquisitionFile(file);
                product.setSipState(SIPState.STORED);
                product.setState(ProductState.FINISHED);
                products.add(product);
                count++;
            }
            productService.save(products);
            LOGGER.info("{}/{} products done", count, MAX_ITEM_PER_LOOP * MAX_LOOPS);

        }
        LOGGER.info("Products(s) created in {} milliseconds", System.currentTimeMillis() - startTime);
    }

    private AcquisitionProcessingChain create(String label) {
        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel(label);
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");
        processingChain.setPeriodicity("0 * * * * *");
        processingChain.setCategories(Sets.newLinkedHashSet());

        // Create an acquisition file info
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo.setDataType(DataType.RAWDATA);
        fileInfo.setScanDirInfo(Sets.newHashSet(new ScanDirectoryInfo(Paths.get("src/resources/doesnotexist"), null)));
        PluginConfiguration scanPlugin = PluginConfiguration.build(GlobDiskScanning.class, null, null);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin");
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginConfiguration.build(DefaultFileValidation.class,
                                                                         null,
                                                                         new HashSet<IPluginParam>());
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin");
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginConfiguration.build(DefaultProductPlugin.class,
                                                                      null,
                                                                      new HashSet<IPluginParam>());
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin");
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginConfiguration.build(DefaultSIPGeneration.class,
                                                                     null,
                                                                     new HashSet<IPluginParam>());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required
        List<StorageMetadataProvider> storages = new ArrayList<>();
        storages.add(StorageMetadataProvider.build("AWS", "/path/to/file", new HashSet<>()));
        storages.add(StorageMetadataProvider.build("HELLO", "/other/path/to/file", new HashSet<>()));
        processingChain.setStorages(storages);

        return processingChain;
    }

    @Test
    public void testDelete() throws ModuleException {
        long expectedDuration = 15_000;
        long startTime = System.currentTimeMillis();
        productService.deleteByProcessingChain(acqProService.getFullChains().get(0));
        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("File(s) deleted by chain in {} milliseconds", duration);
        if (duration > expectedDuration) {
            Assert.fail(String.format(
                "Performance not reached for products deletion by chain .Deletion took %s ms when %s ms expected",
                duration,
                expectedDuration));
        }
    }

    @Test
    public void testDeleteBySession() throws ModuleException {
        long expectedDuration = 30_000;
        long startTime = System.currentTimeMillis();
        productService.deleteBySession(acqProService.getFullChains().get(1), sessionName);
        long duration = System.currentTimeMillis() - startTime;
        LOGGER.info("File(s) deleted by session in {} milliseconds", duration);
        if (duration > expectedDuration) {
            Assert.fail(String.format(
                "Performance not reached for products deletion by session. Deletion took %s ms when %s ms expected",
                duration,
                expectedDuration));
        }
    }

    @Test
    public void checkDeletionRunning() throws ModuleException, InterruptedException, ExecutionException {
        AcquisitionProcessingChain chainToDelete = acqProService.getFullChains().get(2);
        chainToDelete.setActive(false);
        acqProService.updateChain(chainToDelete);
        JobInfo info = productService.scheduleProductsDeletionJob(chainToDelete, Optional.empty(), true);
        Assert.assertTrue(acqProService.isDeletionPending(chainToDelete));
        jobService.runJob(info, getDefaultTenant()).get();
        Thread.sleep(1000);
        Assert.assertFalse(acqProService.isDeletionPending(chainToDelete));
    }

}
