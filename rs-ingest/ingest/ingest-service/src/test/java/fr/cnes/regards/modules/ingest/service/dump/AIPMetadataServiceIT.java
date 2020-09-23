/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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


package fr.cnes.regards.modules.ingest.service.dump;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IDumpConfigurationRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.dump.DumpConfiguration;
import fr.cnes.regards.modules.ingest.domain.exception.NothingToDoException;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.dump.AIPSaveMetadataRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceTest;
import static fr.cnes.regards.modules.ingest.service.TestData.*;
import fr.cnes.regards.modules.ingest.service.aip.IAIPMetadataService;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;

/**
 *
 * @author Iliana Ghazali
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=aip_metadata_service_test",
        "regards.amqp.enabled=true", "regards.dump.zip-limit = 3" },
        locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock", "noschedule" })
public class AIPMetadataServiceIT extends IngestMultitenantServiceTest {

    OffsetDateTime lastDumpReqDate = OffsetDateTime.of(2020, 8, 31, 15, 15, 50, 345875000, ZoneOffset.of("+01:00"));

    private Path tmpZipLocation;

    private DumpConfiguration conf;

    @Value("${regards.dump.zip-limit}")
    private int zipLimit;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    IAIPMetadataService metadataService;

    @Autowired
    private StorageClientMock storageClient;

    @Autowired
    private IDumpConfigurationRepository dumpConf;

    @Override
    public void doInit() {
        simulateApplicationReadyEvent();
        // Re-set tenant because above simulation clear it!
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        abstractRequestRepository.deleteAll();
        jobInfoRepository.deleteAll();
        // init conf
        this.tmpZipLocation = Paths.get("target/tmpZipLocation");
        conf = new DumpConfiguration(true, "", "target/dump",null);
        dumpConf.save(conf);
    }

    @Test
    @Purpose("Test creation of multiple zips between lastDumpReqDate and reqDumpDate")
    public void writeZipsTest() throws NothingToDoException {
        // Params
        int nbSIP = 14; // put at least nbSIP > 1
        int nbAIPToDump = nbSIP * 4 / 5;

        // Create aips and update aip dates
        storageClient.setBehavior(true, true);
        initData(nbSIP);
        updateAIPLastUpdateDate(nbAIPToDump);

        // Create zips
        metadataService.writeZips(createSaveMetadataRequest(), this.tmpZipLocation);

        // CHECK RESULTS
        // Number of zips created
        File[] zipFolder = this.tmpZipLocation.toFile().listFiles();
        int nbZipCreated = zipFolder.length;
        Assert.assertEquals((int) Math.ceil((double) nbAIPToDump / zipLimit), nbZipCreated);

        // Number of files per zip
        int indexZip = 0, nbFiles, totalNbFiles = 0;
        Arrays.sort(zipFolder);
        for (File zipFile : zipFolder) {
            nbFiles = readZipEntryNames(zipFile).size();
            totalNbFiles += nbFiles;
            if (indexZip != nbZipCreated - 1) {
                Assert.assertEquals(this.zipLimit, nbFiles);
            } else {
                Assert.assertEquals(nbAIPToDump % this.zipLimit, nbFiles);
            }
            indexZip++;
        }

        // Total number of aips dumped
        Assert.assertEquals(nbAIPToDump, totalNbFiles);
    }

    @Test
    @Purpose("Test dump of aips is successfully created")
    public void writeDumpTest() throws NothingToDoException {
        // Create aips
        int nbSIP = 14; // put at least nbSIP > 1
        storageClient.setBehavior(true, true);
        initData(nbSIP);

        // Create dump
        AIPSaveMetadataRequest metadataRequest = createSaveMetadataRequest();
        metadataService.writeZips(metadataRequest, this.tmpZipLocation);
        metadataService.writeDump(metadataRequest, Paths.get(metadataRequest.getDumpLocation()), this.tmpZipLocation);

        // CHECK RESULTS
        // Number of dump created
        File[] dumpFolder = Paths.get(this.conf.getDumpLocation()).toFile().listFiles();
        Assert.assertEquals(1, dumpFolder.length);

        // Number of zips in dump
        Assert.assertEquals((int) Math.ceil((double) nbSIP / zipLimit), readZipEntryNames(dumpFolder[0]).size());
    }

    public void initData(int nbSIP) {
        for (int i = 0; i < nbSIP; i++) {
            publishSIPEvent(create(UUID.randomUUID().toString(), getRandomTags()), getRandomStorage().get(0),
                            getRandomSession(), getRandomSessionOwner(), getRandomCategories());
        }
        // Wait
        ingestServiceTest.waitForIngestion(nbSIP, nbSIP * 5000, SIPState.STORED);
    }

    private AIPSaveMetadataRequest createSaveMetadataRequest() {
        // Create request
        AIPSaveMetadataRequest aipSaveMetadataRequest = new AIPSaveMetadataRequest(this.lastDumpReqDate, this.conf.getDumpLocation());
        aipSaveMetadataRequest.setState(InternalRequestState.RUNNING);
        return aipSaveMetadataRequest;
    }

    private void updateAIPLastUpdateDate(int nbAIPToDump) {
        List<AIPEntity> listAip = aipRepository.findAll();
        int index = 1;
        OffsetDateTime refDate = this.lastDumpReqDate.plusDays(nbAIPToDump);
        for (AIPEntity aipEntity : listAip) {
            aipEntity.setCreationDate(refDate.minusDays(index));
            aipEntity.setLastUpdate(refDate.minusDays(index));
            index++;
        }
        aipRepository.saveAll(listAip);
    }

    public List<String> readZipEntryNames(File parentZip) {
        List<String> listNames = new LinkedList<>();
        ZipEntry zipEntry;

        try (ZipFile parentZipFile = new ZipFile(parentZip.getPath())) {
            Enumeration<? extends ZipEntry> zipEntries = parentZipFile.entries();

            while (zipEntries.hasMoreElements()) {
                zipEntry = zipEntries.nextElement();
                listNames.add(zipEntry.getName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return listNames;
    }

    @Override
    protected void doAfter() throws IOException {
        // clean tables (dump + sip + aip + request + job)
        abstractRequestRepository.deleteAll();
        jobInfoRepository.deleteAll();
        aipRepository.deleteAll();
        sipRepository.deleteAll();
        //clear dump location
        FileUtils.deleteDirectory(Paths.get(conf.getDumpLocation()).toFile());
        FileUtils.deleteDirectory(this.tmpZipLocation.toFile());
    }
}