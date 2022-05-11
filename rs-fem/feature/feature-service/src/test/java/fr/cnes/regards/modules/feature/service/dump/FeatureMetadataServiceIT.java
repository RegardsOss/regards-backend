/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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


package fr.cnes.regards.modules.feature.service.dump;

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

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.amqp.event.AbstractRequestEvent;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.dump.domain.DumpParameters;
import fr.cnes.regards.framework.modules.dump.service.settings.DumpSettingsService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.feature.dao.IFeatureSaveMetadataRequestRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.exception.NothingToDoException;
import fr.cnes.regards.modules.feature.domain.request.FeatureSaveMetadataRequest;
import fr.cnes.regards.modules.feature.dto.FeatureRequestStep;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceIT;

/**
 * Test for {@link FeatureMetadataService}
 * @author Iliana Ghazali
 */

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_metadata_service_it",
        "regards.amqp.enabled=true", "regards.feature.dump.zip-limit = 3" })
@ActiveProfiles(value = { "noFemHandler", "noscheduler" })
public class FeatureMetadataServiceIT extends AbstractFeatureMultitenantServiceIT {

    OffsetDateTime lastDumpReqDate = OffsetDateTime.of(2020, 8, 31, 15, 15, 50, 345875000, ZoneOffset.of("+01:00"));

    private Path tmpZipLocation;

    @Value("${regards.feature.dump.zip-limit}")
    private int zipLimit;

    @Autowired
    private IFeatureMetadataService metadataService;

    @Autowired
    private DumpSettingsService dumpSettingsService;

    @Autowired
    private IFeatureSaveMetadataRequestRepository metadataRequestRepository;

    @Override
    public void doInit() throws EntityException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // init conf
        this.tmpZipLocation = Paths.get("target/tmpZipLocation");

        dumpSettingsService.setDumpParameters(new DumpParameters().setActiveModule(true).setDumpLocation("target/dump")
                                                      .setCronTrigger("0 * * * * *"));
    }

    @Test
    @Purpose("Test creation of multiple zips between lastDumpReqDate and reqDumpDate")
    public void writeZipsTest() {
        // Params
        int nbFeatures = 14; // put at least nbFeatures > 1
        int nbFeaturesToDump = nbFeatures * 4 / 5;

        // Create features and update their lastUpdate dates
        initData(nbFeatures);
        updateFeatureLastUpdateDate(nbFeaturesToDump);

        // Create zips
        try {
            metadataService.writeZips(createSaveMetadataRequest(), this.tmpZipLocation);
        } catch (Exception | NothingToDoException e) {
            LOGGER.error("Error occurred while generating of zips", e);
        }

        // CHECK RESULTS
        // Number of zips created
        File[] zipFolder = this.tmpZipLocation.toFile().listFiles();
        int nbZipCreated = zipFolder.length;
        Assert.assertEquals("Unexpected number of created zips",
                            (int) Math.ceil((double) nbFeaturesToDump / zipLimit),
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
                Assert.assertEquals(nbFeaturesToDump % this.zipLimit, nbFiles);
            }
            indexZip++;
        }

        // Total number of feature dumped
        Assert.assertEquals("The number of files created from features is not expected",
                            nbFeaturesToDump,
                            totalNbFiles);
    }

    @Test
    @Purpose("Test dump of features is successfully created")
    public void writeDumpTest() {
        // Create features
        int nbFeatures = 14; // put at least nbFeatures > 1
        initData(nbFeatures);

        // Create dump
        FeatureSaveMetadataRequest metadataRequest = createSaveMetadataRequest();
        try {
            metadataService.writeZips(metadataRequest, this.tmpZipLocation);
            metadataService
                    .writeDump(metadataRequest, Paths.get(metadataRequest.getDumpLocation()), this.tmpZipLocation);
        } catch (NothingToDoException | IOException e) {
            LOGGER.error("Error occurred while dumping features", e);
        }

        // CHECK RESULTS
        // Number of dump created
        File[] dumpFolder = Paths.get(dumpSettingsService.getDumpParameters().getDumpLocation()).toFile().listFiles();
        Assert.assertEquals("Only one dump was expected", 1, dumpFolder.length);

        // Number of zips in dump
        Assert.assertEquals("The number of created zips in dump is not expected",
                            (int) Math.ceil((double) nbFeatures / zipLimit),
                            readZipEntryNames(dumpFolder[0]).size());
    }

    /**
     * Create a request to save feature metadata
     * @return FeatureSaveMetadataRequest
     */
    private FeatureSaveMetadataRequest createSaveMetadataRequest() {
        // Create request
        return FeatureSaveMetadataRequest.build(AbstractRequestEvent.generateRequestId(),
                                                "NONE",
                                                OffsetDateTime.now(),
                                                RequestState.GRANTED,
                                                null,
                                                FeatureRequestStep.LOCAL_SCHEDULED,
                                                PriorityLevel.NORMAL,
                                                this.lastDumpReqDate,
                                                dumpSettingsService.getDumpParameters().getDumpLocation());
    }

    /**
     * Update feature last dates by decreasing the number of days
     * @param nbFeatureToDump number of features to update
     */
    private void updateFeatureLastUpdateDate(int nbFeatureToDump) {
        List<FeatureEntity> listFeatures = featureRepo.findAll();
        int index = 1;
        OffsetDateTime refDate = this.lastDumpReqDate.plusDays(nbFeatureToDump);
        for (FeatureEntity featureEntity : listFeatures) {
            featureEntity.setCreationDate(refDate.minusDays(index));
            featureEntity.setLastUpdate(refDate.minusDays(index));
            index++;
        }
        featureRepo.saveAll(listFeatures);
    }

    /**
     * List all filenames contained in a zip
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
    protected void doAfter() throws IOException {
        abstractFeatureRequestRepo.deleteAll();
        featureRepo.deleteAll();
        //clear dump location
        FileUtils.deleteDirectory(Paths.get(dumpSettingsService.getDumpParameters().getDumpLocation()).toFile());
        FileUtils.deleteDirectory(this.tmpZipLocation.toFile());
    }
}
