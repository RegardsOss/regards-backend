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


package fr.cnes.regards.modules.ingest.service.dump;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.dump.service.settings.DumpSettingsService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.exception.NothingToDoException;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.dump.AIPSaveMetadataRequest;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
import fr.cnes.regards.modules.ingest.service.aip.scheduler.IngestRequestSchedulerService;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Test for {@link AIPMetadataService}
 *
 * @author Iliana Ghazali
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=aip_metadata_service_it",
                                   "regards.amqp.enabled=true",
                                   "regards.aip.dump.zip-limit = 3" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock", "noscheduler" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class AIPMetadataServiceIT extends IngestMultitenantServiceIT {

    OffsetDateTime lastDumpReqDate = OffsetDateTime.of(2020, 8, 31, 15, 15, 50, 345875000, ZoneOffset.of("+01:00"));

    private static final Path TMP_ZIP_LOCATION = Paths.get("target/tmpZipLocation");

    private static final Path TARGET_ZIP_LOCATION = Paths.get("target/dump");

    @Value("${regards.aip.dump.zip-limit}")
    private int zipLimit;

    @Autowired
    private IAIPMetadataService metadataService;

    @Autowired
    private StorageClientMock storageClient;

    @Autowired
    private DumpSettingsService dumpSettingsService;

    @Autowired
    private IngestRequestSchedulerService ingestRequestSchedulerService;

    @Test
    @Purpose("Test creation of multiple zips between lastDumpReqDate and reqDumpDate")
    public void writeZipsTest() {
        // Params
        int nbSIP = 14; // put at least nbSIP > 1
        int nbAIPToDump = nbSIP * 4 / 5;

        // Create aips and update aip dates
        storageClient.setBehavior(true, true);
        initRandomData(nbSIP, ingestRequestSchedulerService);
        updateAIPLastUpdateDate(nbAIPToDump);

        // Create zips
        try {
            metadataService.writeZips(createSaveMetadataRequest(), TMP_ZIP_LOCATION);
        } catch (Exception | NothingToDoException e) {
            LOGGER.error("Error occurred while generating of zips", e);
        }

        // CHECK RESULTS
        // Number of zips created
        File[] zipFolder = TMP_ZIP_LOCATION.toFile().listFiles();
        int nbZipCreated = zipFolder.length;
        Assert.assertEquals("Unexpected number of created zips",
                            (int) Math.ceil((double) nbAIPToDump / zipLimit),
                            nbZipCreated);

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
        Assert.assertEquals("The number of files created from aips is not expected", nbAIPToDump, totalNbFiles);
    }

    @Test
    @Purpose("Test dump of aips is successfully created")
    public void writeDumpTest() {
        // Create aips
        int nbSIP = 14; // put at least nbSIP > 1
        storageClient.setBehavior(true, true);
        initRandomData(nbSIP, ingestRequestSchedulerService);

        // Create dump
        AIPSaveMetadataRequest metadataRequest = createSaveMetadataRequest();
        try {
            metadataService.writeZips(metadataRequest, TMP_ZIP_LOCATION);
            metadataService.writeDump(metadataRequest, Paths.get(metadataRequest.getDumpLocation()), TMP_ZIP_LOCATION);
        } catch (NothingToDoException | IOException e) {
            LOGGER.error("Error occurred while dumping aips", e);
        }

        // CHECK RESULTS
        // Number of dump created
        File[] dumpFolder = TARGET_ZIP_LOCATION.toFile().listFiles();
        Assert.assertEquals("Only one dump was expected", 1, dumpFolder.length);

        // Number of zips in dump
        Assert.assertEquals("The number of created zips in dump is not expected",
                            (int) Math.ceil((double) nbSIP / zipLimit),
                            readZipEntryNames(dumpFolder[0]).size());
    }

    /**
     * Create a request to save aip metadata
     *
     * @return AIPSaveMetadataRequest
     */
    private AIPSaveMetadataRequest createSaveMetadataRequest() {
        // Create request
        AIPSaveMetadataRequest aipSaveMetadataRequest = new AIPSaveMetadataRequest(this.lastDumpReqDate,
                                                                                   TARGET_ZIP_LOCATION.toString());
        aipSaveMetadataRequest.setState(InternalRequestState.RUNNING);
        return aipSaveMetadataRequest;
    }

    /**
     * Update aip last dates by decreasing the number of days
     *
     * @param nbAIPToDump number of aips to update
     */
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

    /**
     * List all filenames contained in a zip
     *
     * @param parentZip zip to scan
     * @return list of filenames
     */
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
    protected void doAfter() throws IOException, EntityException {
        //clear dump location
        FileUtils.deleteDirectory(TARGET_ZIP_LOCATION.toFile());
        FileUtils.deleteDirectory(TMP_ZIP_LOCATION.toFile());
        dumpSettingsService.resetSettings();
    }
}