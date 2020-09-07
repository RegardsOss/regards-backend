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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static fr.cnes.regards.framework.dump.TestUtils.*;
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

    @Test
    @Purpose("Verify the creation of object dump zips inside of a global zip")
    public void testGenerateJsonZips() throws IOException {

        // PREPARE AND LAUNCH TESTS
        // parameters
        int numOfJson = 16;
        int maxFilesPerSubZip = 4;

        // create test data
        ArrayList<ObjectDump> dumpCollection = TestData.buildJsonCollection(numOfJson);

        // create zip files
        String zipLocation = "target/dump";
        File zipFolder = new File(zipLocation);

        // delete zip folder is previously generated
        if (Files.exists(zipFolder.toPath())) {
            deleteDir(zipFolder);
        }

        // launch test
        dumpService.generateJsonZips(dumpCollection, zipLocation);

        // CHECK RESULTS
        // regroup object dumps per sets of regards.json.dump.max.per.sub.zip
        List<List<ObjectDump>> sets = TestUtils
                .createSets(dumpCollection, dumpCollection.size(), maxFilesPerSubZip); // Nb of sets expected = 4

        // check if destination folder was created
        File[] listZip = zipFolder.listFiles();
        Assert.assertFalse("target dir should exists and contains only one zip",
                           !Files.exists(zipFolder.toPath()) && listZip.length != 1);

        // check name of created zip
        File parentFileZip = listZip[0];
        String errorMsg = checkZipCreation(parentFileZip);
        Assert.assertFalse(errorMsg, !errorMsg.isEmpty());

        // check subzips creation
        LinkedList<String> errorReasons = checkSubZipCreation(parentFileZip, sets);
        Assert.assertFalse("The zip was not created correctly. Reasons : " + errorReasons, !errorReasons.isEmpty());
    }

    @Test
    @Purpose("Verify object dumps in error if json names are not unique")
    public void testErrorDumps() throws IOException {
        // PREPARE AND LAUNCH TESTS
        // parameters
        int numOfJson = 11;

        // create test data with all dumps in error except one
        ArrayList<ObjectDump> dumpCollection = TestData.buildErrorJsonCollection(numOfJson);

        // create zip files
        String zipLocation = "target/dumperrors";
        File zipFolder = new File(zipLocation);

        // delete zip folder is previously generated
        if (Files.exists(zipFolder.toPath())) {
            deleteDir(zipFolder);
        }

        // launch test
        List<ObjectDump> errorDumps = dumpService.generateJsonZips(dumpCollection, zipLocation);

        //check that all dumps are in error
        Assert.assertEquals(numOfJson, errorDumps.size());

    }

}
