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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;

/**
 *
 * @author Iliana Ghazali
 */
//TODO rename DumpService + annotate Service for spring support
@Service
public class DumpService {

    @Autowired
    private Gson gson;
    //TODO autowired Gson => dependence vers gson-regards-starter - X --> ??

    public static final Logger LOGGER = LoggerFactory.getLogger(DumpService.class);

    private static final int MAX_FILES_PER_ZIP = 1000;

    private String folderPathPattern = "yyyy/MM/dd";

    @Value("${spring.application.name}")
    private String microservice;

    /**
     * Create a zip archive of multiple zips.
     * @param dumpCollection data to zip
     * @param zipLocation
     */
    public void generateJsonZips(List<ObjectDump> dumpCollection, String zipLocation) throws IOException {
        // check unique name
        // Locals Vars
        DateTimeFormatter folderPathFormatter = DateTimeFormatter.ofPattern(folderPathPattern);
        //FIXME use same format than everything else in REGARDS (OffsetDateTimeAdapter) - IMPLEMENTED --> OffsetDateTimeAdapter imported with mvn

        String firstDate, lastDate, zipName, filePath, filename, fileContent;
        ZipEntry zipEntry;
        ObjectDump currentFile;
        ArrayList<String> zipCreatedList = new ArrayList<>();
        int indexName, startSetIndex, endSetIndex;

        //Params
        int nbFiles = dumpCollection.size();
        int nbSets = (int) Math.ceil((double) nbFiles / MAX_FILES_PER_ZIP);
        int nbFilesInSet = MAX_FILES_PER_ZIP;

        // Sort dump collection by date
        Collections.sort(dumpCollection);

        List<List<ObjectDump>> zipGlobal = new ArrayList<>();

        // Check if zip location exists
        Files.createDirectories(Paths.get(zipLocation));

        // FIXME try/catch with outputstreams - IMPLEMENTED --> TO CHECK
        // Create Root Zip
        lastDate = OffsetDateTimeAdapter.format(dumpCollection.get(nbFiles - 1).getCreationDate());
        zipName = "dump_json_" + microservice + "_" + lastDate;
        try (ZipOutputStream rootZip = new ZipOutputStream(
                new FileOutputStream(new File(zipLocation + "/" + zipName + ".zip")));) {

            for(List<ObjectDump> zipInterrieur: zipGlobal) {
            //for each sets of files
                for(ObjectDump file: zipInterrieur) {
            for (int numSet = 0; numSet < nbSets; numSet++) {

                if ((numSet + 1) * MAX_FILES_PER_ZIP > nbFiles) {
                    nbFilesInSet = nbFiles % MAX_FILES_PER_ZIP;
                }

                // Create Sub Zip
                try (ByteArrayOutputStream subZipByte = new ByteArrayOutputStream();
                        ZipOutputStream subZip = new ZipOutputStream(new BufferedOutputStream(subZipByte));) {
                    startSetIndex = numSet * MAX_FILES_PER_ZIP;
                    endSetIndex = numSet * MAX_FILES_PER_ZIP + nbFilesInSet - 1;

                    // Retrieve first and last date of set
                    zipInterrieur.get(0);
                            zipInterrieur.get(zipInterrieur.size()-1);
                    firstDate = OffsetDateTimeAdapter.format(dumpCollection.get(startSetIndex).getCreationDate());
                    lastDate = OffsetDateTimeAdapter.format(dumpCollection.get(endSetIndex).getCreationDate());
                    zipName = firstDate + "_" + lastDate + ".zip";

                    // Handle not unique zip names by adding index
                    if (zipCreatedList.contains(zipName)) {
                        indexName = 0;
                        do {
                            zipName = firstDate + "_" + lastDate + "_" + indexName + ".zip";
                            indexName++;
                        } while (zipCreatedList.contains(zipName));
                    }
                    zipCreatedList.add(zipName);

                    // Add Files to Sub Zip
                    for (int numFile = 0; numFile < nbFilesInSet; numFile++) {
                        currentFile = dumpCollection.get(startSetIndex + numFile);
                        filename = currentFile.getJsonName() + ".json";
                        filePath = folderPathFormatter.format(currentFile.getCreationDate()) + "/" + filename;
                        fileContent = this.gson.toJson(currentFile.getJsonContent());
                        //Add File to Sub Zip
                        zipEntry = new ZipEntry(filePath);
                        subZip.putNextEntry(zipEntry);
                        subZip.write(fileContent.getBytes());
                        subZip.closeEntry();
                    }

                    // Close Sub Zip
                    subZip.close();

                    // Add Sub Zip to Root Zip
                    zipEntry = new ZipEntry(zipName);
                    rootZip.putNextEntry(zipEntry);
                    rootZip.write(subZipByte.toByteArray());
                    rootZip.closeEntry();
                } catch (Exception e) {
                    LOGGER.error("Error while generating zip at {}", zipLocation, e);
                    return;
                }
            }
        }
    }

}

