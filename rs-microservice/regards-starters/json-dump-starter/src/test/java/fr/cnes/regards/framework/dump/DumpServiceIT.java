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


package fr.cnes.regards.framework.dump;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static fr.cnes.regards.framework.dump.TestUtils.checkZipCreation;
import static fr.cnes.regards.framework.dump.TestUtils.readZipEntryNames;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;

/**
 *
 * @author Iliana Ghazali
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ComponentScan(basePackages = { "fr.cnes.regards.framework" })
@EnableAutoConfiguration
@TestPropertySource(properties = { "spring.application.name=test", "regards.json.dump.max.per.sub.zip=4",
        "regards.dump.location=target/dump" })
public class DumpServiceIT {

    @Autowired
    private DumpService dumpService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Value("${regards.json.dump.max.per.sub.zip}")
    private int maxFilesPerSubZip;

    @Value("${spring.application.name}")
    private String microservice;

    @Value("${regards.dump.location}")
    private String dumpLocation;

    @Test
    @Purpose("Test creation of a dump")
    public void generateDumpTest() throws IOException {
        // ------------------------------ PREPARE AND LAUNCH TESTS ------------------------------
        // define workspace and force tenant
        String tenantName = "test";
        runtimeTenantResolver.forceTenant(tenantName);
        this.dumpLocation += "/" + tenantName;

        // delete folders if previously generated
        Path dumpLocationPath = Paths.get(this.dumpLocation);
        if (Files.exists(dumpLocationPath)) {
            try {
                FileUtils.deleteDirectory(dumpLocationPath.toFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // generate zips in dumpLocation
        int nbZips = 3;
        ArrayList<ObjectDump> zipCollection;
        for (int i = 0; i < nbZips; i++) {
            zipCollection = TestData.buildJsonCollection(this.maxFilesPerSubZip);
            dumpService.generateJsonZip(zipCollection, dumpLocationPath);
        }
        // Get list of created zips
        List<String> listValidNames = Arrays.stream(dumpLocationPath.toFile().listFiles()).map(file -> file.getName())
                .collect(Collectors.toList());

        // Generate Dump
        OffsetDateTime creationDate = OffsetDateTime.now();
        dumpService.generateDump(dumpLocationPath, creationDate);

        // ----------------------------------- CHECK RESULTS -----------------------------------
        // check if dumpFolder was created
        Assert.assertTrue("dir and dump dir should exist", Files.exists(dumpLocationPath));

        // check if dumpFolder has only one zip
        File[] listDump = dumpLocationPath.toFile().listFiles();
        Assert.assertEquals(1, listDump.length);

        // check name of dump zip
        File dump = listDump[0];
        String dumpName = dump.getName();
        String zipNameRegex =
                "^dump_json_" + this.microservice + "_" + OffsetDateTimeAdapter.format(creationDate) + "\\.zip$";
        Assert.assertFalse("The name of the created zip \"" + dumpName
                                   + "\" is incorrect and should match the following pattern : dump_json_microservice_dumpdate ("
                                   + zipNameRegex + ")", !dumpName.matches(zipNameRegex));

        // check the number of zip entries in dumpZip
        List<String> listZipNames = readZipEntryNames(dump);
        Assert.assertEquals(listValidNames.size(), listZipNames.size());

        // check if zip names correspond to those expected
        String validZipName, createdZipName;
        for (int i = 0; i < nbZips; i++) {
            validZipName = listValidNames.get(i);
            createdZipName = listZipNames.get(i);
            Assert.assertEquals(validZipName, createdZipName);
        }
    }

    @Test
    @Purpose("Test the creation of a zip")
    public void generateJsonZipTest() throws IOException {
        // ------------------------------ PREPARE AND LAUNCH TESTS ------------------------------
        // create test data
        ArrayList<ObjectDump> zipCollection = TestData.buildJsonCollection(this.maxFilesPerSubZip);
        // define workspace and force tenant
        String tenantName = "test";
        runtimeTenantResolver.forceTenant(tenantName);
        this.dumpLocation += "/" + tenantName;
        // create zip files
        Path dumpLocationPath = Paths.get(this.dumpLocation);
        File dumpFolder = dumpLocationPath.toFile();
        // delete zip folder is previously generated
        if (Files.exists(dumpLocationPath)) {
            try {
                FileUtils.deleteDirectory(dumpFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // launch test
        dumpService.generateJsonZip(zipCollection, dumpLocationPath);

        // ----------------------------------- CHECK RESULTS -----------------------------------
        // check if destination folder was created
        Assert.assertTrue("target dir should exist", Files.exists(dumpLocationPath));

        // check if zip was created
        File[] listZip = dumpFolder.listFiles();
        Assert.assertEquals(1, listZip.length);

        // check name of created zip
        File zip = listZip[0];
        String errorMsg = checkZipCreation(zip, zipCollection);
        Assert.assertTrue("The zip was not created properly. Reason: " + errorMsg, errorMsg.isEmpty());
    }

    @Test
    @Purpose("Verify object dumps in error if json names are not unique")
    public void testDuplicatedDumps() {
        // create test data with all dumps in error
        int numOfJson = 11;
        ArrayList<ObjectDump> dumpCollection = TestData.buildDuplicatedJsonCollection(numOfJson);
        // launch test
        List<ObjectDump> duplicatedDumps = dumpService.checkUniqueJsonNames(dumpCollection);
        // check that all dumps are in error
        Assert.assertEquals(numOfJson, duplicatedDumps.size());
    }
}
