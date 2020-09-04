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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static fr.cnes.regards.framework.dump.TestUtils.deleteDir;
import static fr.cnes.regards.framework.dump.TestUtils.readZipEntries;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;

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
    public void testGenerateJsonZips() throws IOException {

        // PREPARE AND LAUNCH TESTS
        // parameters
        int numOfJson = 16;
        int maxFilesPerSubZip = 4;

        //create test data
        ArrayList<ObjectDump> dumpCollection = TestData.buildJsonCollection(numOfJson);

        //create zip files
        String zipLocation = "target/dump";
        File zipFolder = new File(zipLocation);

        // Tests
        if (Files.exists(zipFolder.toPath())) {
            deleteDir(zipFolder);
        }

        //launch test
        dumpService.generateJsonZips(dumpCollection, zipLocation);


        //prepare vars for test

        List<List<ObjectDump>> sets = TestUtils.createSets(dumpCollection, dumpCollection.size(), maxFilesPerSubZip); // Nb of sets expected = 4
        File[] listZip = zipFolder.listFiles();
        File parentFileZip = listZip[0];

        // CHECK RESULTS
        // Check if destination folder was created
        Assert.assertTrue("target dir should exists", Files.exists(zipFolder.toPath()));

        // Check if a unique zip was created
        checkSubZipCreation(parentFileZip,TestData.getDateSet(),maxFilesPerSubZip);
        //Assert.assertTrue("zip file was not created or name is incorrect",checkZipCreation(zipFolder));


        // Check path created in folder


    }

    private boolean checkZipCreation(File[] listZip,File parentFileZip){
        String dateRegex = "((?:[1-9][0-9]*)?[0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])\\.([0-9]{1,6})Z";
        String microserviceProperty = "test";
        String zipNameRegex = "^dump_json_" + microserviceProperty + "_" + dateRegex + "\\.zip$";
        return listZip.length == 1 && parentFileZip.getName().matches(zipNameRegex);
    }

    private boolean checkSubZipCreation(File parentFileZip, ArrayList<OffsetDateTime> dateSet, int maxFilesPerSubZip){
        boolean flagError = false;
        int counter = 0;
        String dateFormatted;
        Map<String, List<ZipEntry>> mapSubZips = readZipEntries(parentFileZip);
        ZipEntry fileEntry;

        //regex for name of json file
        String dateRegex = "((?:[1-9][0-9]*)?[0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])\\.([0-9]{1,6})Z";
        String subZipNamePattern = dateRegex + "_" + dateRegex + "(_[0-9]+)?\\.zip$";

        //format date
        LinkedList<String> dateSetFormatted = new LinkedList<>();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        Collections.sort(dateSet);
        for(OffsetDateTime date : dateSet){
            dateSetFormatted.add(dateTimeFormatter.format(date));
        }
        int counterDateFormatted = 0;
        dateFormatted = dateSetFormatted.get(counterDateFormatted);

        //for each subzip - check size and name of files
        Iterator<Map.Entry<String, List<ZipEntry>>> subZipIt = mapSubZips.entrySet().iterator();
        Map.Entry<String, List<ZipEntry>> subZip;
        String subZipName;
        List<ZipEntry> subZipFiles;

        Iterator fileIt;
        while(subZipIt.hasNext() && !flagError) {
            subZip = subZipIt.next();
            subZipName = subZip.getKey();
            subZipFiles = subZip.getValue();
            if (subZipFiles.size() > maxFilesPerSubZip || !subZipName.matches(subZipNamePattern)) {
                flagError = true;
            }
            fileIt = subZipFiles.iterator();
            while(fileIt.hasNext() && !flagError) {
                counter++;
                fileEntry = (ZipEntry) fileIt.next();
                fileEntry.getName();
                //fileEntry.getName().matches(subZipName);
                if(counter==maxFilesPerSubZip-1){
                    dateFormatted = dateSetFormatted.get(counterDateFormatted % maxFilesPerSubZip);
                    counterDateFormatted++;
                    counter = 0;
                }

            }
        }
        return flagError;
    }

}
