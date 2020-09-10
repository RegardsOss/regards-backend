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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

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
import fr.cnes.regards.framework.test.report.annotation.Purpose;

/**
 *
 * @author Iliana Ghazali
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ComponentScan(basePackages = { "fr.cnes.regards.framework" })
@EnableAutoConfiguration
@TestPropertySource(properties = { "spring.application.name=test", "regards.json.dump.max.per.sub.zip=4" })
public class DumpServiceIT {

    @Autowired
    private DumpService dumpService;

    @Value("${regards.json.dump.max.per.sub.zip}")
    private int maxFilesPerSubZip;

    @Value("${spring.application.name}")
    private String microservice;

    @Test
    @Purpose("Test creation of a dump")
    public void generateDumpTest() throws IOException {
        // ------------------------------ PREPARE AND LAUNCH TESTS ------------------------------
        // create zip files
        String tmpDumpLocation = "target/tmpdump", dumpLocation = "target/dump";
        File tmpDumpFolder = new File(tmpDumpLocation), dumpFolder = new File(dumpLocation);
        // delete folders if previously generated
        try {
            if (Files.exists(tmpDumpFolder.toPath())) {
                FileUtils.deleteDirectory(tmpDumpFolder);
            }
            if (Files.exists(dumpFolder.toPath())) {
                FileUtils.deleteDirectory(dumpFolder);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // generate zips in tmpDumpLocation
        int nbZips = 3;
        ArrayList<ObjectDump> zipCollection;
        for (int i = 0; i < nbZips; i++) {
            zipCollection = TestData.buildJsonCollection(this.maxFilesPerSubZip);
            dumpService.generateJsonZip(zipCollection, tmpDumpLocation);
        }
        // Generate Dump
        OffsetDateTime creationDate = OffsetDateTime.now();
        dumpService.generateDump(dumpLocation, tmpDumpLocation, creationDate);

        // ----------------------------------- CHECK RESULTS -----------------------------------
        // check if tmpFolder and dumpFolder were created
        Assert.assertTrue("tmp dir and dump dir should exist",
                          Files.exists(tmpDumpFolder.toPath()) && Files.exists(dumpFolder.toPath()));

        // check if tmpDumpFolder has the right number of zips
        File[] listZip = tmpDumpFolder.listFiles();
        Assert.assertFalse("tmp dir should contain " + nbZips + " zips(s)", listZip.length != nbZips);

        // check if dumpFolder has only one zip
        File[] listDump = dumpFolder.listFiles();
        Assert.assertFalse("dump dir should contain only one zip", listDump.length != 1);

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
        Assert.assertFalse("The dump zip does not contain all required zips",
                           listZipNames.size() != tmpDumpFolder.listFiles().length);

        // check if zip names correspond to those expected
        String validZipName, createdZipName;
        for (int i = 0; i < nbZips; i++) {
            validZipName = listZip[i].getName();
            createdZipName = listZipNames.get(i);
            Assert.assertFalse(
                    "The zip name \"" + createdZipName + "\" does not match the expected name \"" + validZipName + "\"",
                    !createdZipName.equals(validZipName));
        }
    }

    @Test
    @Purpose("Test the creation of a zip")
    public void generateJsonZipTest() throws IOException {
        // ------------------------------ PREPARE AND LAUNCH TESTS ------------------------------
        // create test data
        ArrayList<ObjectDump> zipCollection = TestData.buildJsonCollection(this.maxFilesPerSubZip);
        // create zip files
        String tmpDumpLocation = "target/tmpdump";
        File tmpDumpFolder = new File(tmpDumpLocation);
        // delete zip folder is previously generated
        if (Files.exists(tmpDumpFolder.toPath())) {
            try {
                FileUtils.deleteDirectory(tmpDumpFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // launch test
        dumpService.generateJsonZip(zipCollection, tmpDumpLocation);

        // ----------------------------------- CHECK RESULTS -----------------------------------
        // check if destination folder was created
        Assert.assertTrue("target dir should exist", Files.exists(tmpDumpFolder.toPath()));

        // check if zip was created
        File[] listZip = tmpDumpFolder.listFiles();
        Assert.assertFalse("target dir should contain only one zip", listZip.length != 1);

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
        List<String> duplicatedDumps = dumpService.checkUniqueJsonNames(dumpCollection);
        // check that all dumps are in error
        Assert.assertEquals(numOfJson, duplicatedDumps.size());
    }
}
