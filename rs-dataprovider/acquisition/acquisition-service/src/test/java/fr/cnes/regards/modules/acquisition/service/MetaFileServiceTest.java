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
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.acquisition.builder.MetaFileBuilder;
import fr.cnes.regards.modules.acquisition.dao.IMetaFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IScanDirectoryRepository;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;
import fr.cnes.regards.modules.acquisition.service.conf.AcquisitionServiceConfiguration;

/**
 * @author Christophe Mertz
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AcquisitionServiceConfiguration.class })
@ActiveProfiles({ "test", "disableDataProviderTask" })
@Transactional
@DirtiesContext
public class MetaFileServiceTest {

    @Value("${regards.tenant}")
    private String tenant;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IScanDirectoryService scandirService;

    @Autowired
    private IScanDirectoryRepository scanDirectoryRepository;

    @Autowired
    private IMetaFileService metaFileService;

    @Autowired
    private IMetaFileRepository metaFileRepository;

    @BeforeTransaction
    protected void beforeTransaction() {
        tenantResolver.forceTenant(tenant);
    }

    @Before
    public void cleanDb() {
        scanDirectoryRepository.deleteAll();
        metaFileRepository.deleteAll();
    }

    @Test
    public void createAndUpdateOneScanDir() throws ModuleException {
        String dirName = "/etc/alternatives";

        Assert.assertEquals(0, scanDirectoryRepository.count());

        // create first scan dir
        ScanDirectory scanDir1 = new ScanDirectory(dirName);
        ScanDirectory created = scandirService.createOrUpdate(scanDir1);

        Assert.assertEquals(1, scanDirectoryRepository.count());
        Assert.assertEquals(dirName, scandirService.retrieve(scanDir1.getId()).getScanDir());
        Assert.assertEquals(created, scanDir1);

        // update existing scan dir with a modification
        String newDirName = "/var/log/data/file/test/spool";
        scanDir1.setScanDir(newDirName);
        ScanDirectory updated = scandirService.createOrUpdate(scanDir1);

        Assert.assertEquals(1, scandirService.retrieveAll().size());
        Assert.assertEquals(newDirName, scandirService.retrieve(scanDir1.getId()).getScanDir());
        Assert.assertEquals(updated, scanDir1);

        // update existing scan dir without modification
        updated = scandirService.createOrUpdate(scanDir1);

        Assert.assertEquals(1, scanDirectoryRepository.count());
        Assert.assertEquals(newDirName, scandirService.retrieve(scanDir1.getId()).getScanDir());
        Assert.assertEquals(updated, scanDir1);

        scandirService.delete(scanDir1.getId());
        Assert.assertEquals(0, scanDirectoryRepository.count());
    }

    @Test
    public void createAndUpdateSetOfScanDir() throws ModuleException {
        String dirName = "/usr/local/sbin";

        Assert.assertEquals(0, scanDirectoryRepository.count());

        // create many scan dir
        ScanDirectory scanDir1 = new ScanDirectory(dirName + "/one");
        ScanDirectory scanDir2 = new ScanDirectory(dirName + "/two");
        ScanDirectory scanDir3 = new ScanDirectory(dirName + "/three");
        scandirService.createOrUpdate(scanDir1);
        scandirService.createOrUpdate(scanDir2);
        scandirService.createOrUpdate(scanDir3);

        Assert.assertEquals(3, scanDirectoryRepository.count());

        // update one existing scan dir with a modification
        String newDirName = "/var/log/data/file/test/spool";
        scanDir2.setScanDir(newDirName);
        Set<ScanDirectory> scanDirs = new HashSet<>();
        scanDirs.add(scanDir1);
        scanDirs.add(scanDir2);
        scanDirs.add(scanDir3);
        scandirService.createOrUpdate(scanDirs);

        Assert.assertEquals(3, scanDirectoryRepository.count());
        Assert.assertEquals(newDirName, scandirService.retrieve(scanDir2.getId()).getScanDir());
        Assert.assertEquals(dirName + "/one", scandirService.retrieve(scanDir1.getId()).getScanDir());
        Assert.assertEquals(dirName + "/three", scandirService.retrieve(scanDir3.getId()).getScanDir());

        // add a new scan dir to the Set
        ScanDirectory scanDir4 = new ScanDirectory(dirName + "/for");
        scanDirs.add(scanDir4);
        Set<ScanDirectory> createdScanDirs = scandirService.createOrUpdate(scanDirs);

        Assert.assertEquals(4, createdScanDirs.size());
        Assert.assertTrue(createdScanDirs.contains(scanDir4));
        Assert.assertEquals(4, scanDirectoryRepository.count());
        Assert.assertEquals(newDirName, scandirService.retrieve(scanDir2.getId()).getScanDir());
        Assert.assertEquals(dirName + "/one", scandirService.retrieve(scanDir1.getId()).getScanDir());
        Assert.assertEquals(dirName + "/three", scandirService.retrieve(scanDir3.getId()).getScanDir());
        Assert.assertEquals(dirName + "/for", scandirService.retrieve(scanDir4.getId()).getScanDir());

        // remove a scan dir
        scanDirs.remove(scanDir3);
        scanDirs.remove(scanDir4);
        scanDir4.setScanDir(newDirName + "/for");
        scanDirs.add(scanDir4);
        createdScanDirs = scandirService.createOrUpdate(scanDirs, Sets.newHashSet(scandirService.retrieveAll()));
        Assert.assertEquals(3, createdScanDirs.size());
        Assert.assertFalse(createdScanDirs.contains(scanDir3));
        Assert.assertTrue(createdScanDirs.contains(scanDir4));
        Assert.assertEquals(3, scanDirectoryRepository.count());
        Assert.assertEquals(newDirName, scandirService.retrieve(scanDir2.getId()).getScanDir());
        Assert.assertEquals(dirName + "/one", scandirService.retrieve(scanDir1.getId()).getScanDir());
        Assert.assertEquals(newDirName + "/for", scandirService.retrieve(scanDir4.getId()).getScanDir());
    }

    @Test
    public void createAndUpdateOneMetaFile() throws ModuleException {
        MetaFile metaFile1 = MetaFileBuilder.build().isMandatory().withFilePattern("pattern1").withFileType("file type")
                .withInvalidFolder("tmp/invalid1").get();
        MetaFile metaFile2 = MetaFileBuilder.build().withFilePattern("pattern2").withFileType("file type")
                .withInvalidFolder("tmp/invalid2").get();
        MetaFile metaFile3 = MetaFileBuilder.build().isMandatory().withFilePattern("pattern3").withFileType("file type")
                .withInvalidFolder("tmp/invalid3").get();
        metaFileService.createOrUpdate(metaFile1);
        metaFileService.createOrUpdate(metaFile2);
        metaFileService.createOrUpdate(metaFile3);

        Assert.assertEquals(3, metaFileRepository.count());

        String newPattern = "a new pattern";
        metaFile3.setFileNamePattern(newPattern);
        metaFileService.createOrUpdate(metaFile3);

        Assert.assertEquals(3, metaFileRepository.count());
        Assert.assertEquals(newPattern, metaFileService.retrieve(metaFile3.getId()).getFileNamePattern());
        Assert.assertEquals("tmp/invalid3", metaFileService.retrieve(metaFile3.getId()).getInvalidFolder());
        Assert.assertEquals("pattern2", metaFileService.retrieve(metaFile2.getId()).getFileNamePattern());
        Assert.assertEquals("pattern1", metaFileService.retrieve(metaFile1.getId()).getFileNamePattern());

        metaFileService.delete(metaFile1);
        Assert.assertEquals(2, metaFileRepository.count());

        metaFile2.setComment("a new comment");
        metaFile2.setFileType("application/pdf");
        metaFile2.setMandatory(true);

        metaFileService.update(metaFile2.getId(), metaFile2);
        Assert.assertEquals(metaFile2, metaFileService.retrieve(metaFile2.getId()));

        metaFileService.delete(metaFile2.getId());
        Assert.assertEquals(1, metaFileRepository.count());
    }

    @Test
    public void createAndUpdateSetOfMetaFile() throws ModuleException {
        String dirName = "/var/log";

        // create a set of MetaFile
        MetaFile metaFile1 = MetaFileBuilder.build().isMandatory().withFilePattern("pattern1").withFileType("file type")
                .withInvalidFolder("tmp/invalid1").get();
        MetaFile metaFile2 = MetaFileBuilder.build().withFilePattern("pattern2").withFileType("file type")
                .withInvalidFolder("tmp/invalid2").get();
        MetaFile metaFile3 = MetaFileBuilder.build().isMandatory().withFilePattern("pattern3").withFileType("file type")
                .withInvalidFolder("tmp/invalid3").get();

        Set<MetaFile> metaFiles = new HashSet<>();
        metaFiles.add(metaFile1);
        metaFiles.add(metaFile2);
        metaFiles.add(metaFile3);

        metaFileService.createOrUpdate(metaFiles);

        Assert.assertEquals(3, metaFileRepository.count());

        // Modify a MetaFile and update the set of MetaFile
        metaFiles.remove(metaFile2);
        metaFile2.addScanDirectory(new ScanDirectory(dirName + "/one"));
        metaFile2.addScanDirectory(new ScanDirectory(dirName + "/two"));
        String newPattern = "a new pattern for metafile 2";
        metaFile2.setFileNamePattern(newPattern);
        metaFiles.add(metaFile2);

        metaFileService.createOrUpdate(metaFiles);

        Assert.assertEquals(3, metaFileRepository.count());
        Assert.assertEquals(2, scanDirectoryRepository.count());

        Assert.assertEquals(metaFile2, metaFileService.retrieve(metaFile2.getId()));

        // Modify a MetaFile and update the set of MetaFile
        metaFiles.remove(metaFile2);
        metaFile2.addScanDirectory(new ScanDirectory(dirName + "/three"));
        metaFiles.add(metaFile2);

        metaFileService.createOrUpdate(metaFiles);

        Assert.assertEquals(3, metaFileRepository.count());
        Assert.assertEquals(3, scanDirectoryRepository.count());

        Assert.assertEquals(metaFile2, metaFileService.retrieve(metaFile2.getId()));

    }

}
