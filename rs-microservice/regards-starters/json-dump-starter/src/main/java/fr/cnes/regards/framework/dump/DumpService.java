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
import java.time.OffsetDateTime;
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

import com.google.common.collect.*;
import com.google.gson.Gson;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;

/**
 *
 * @author Iliana Ghazali
 */
@Service
public class DumpService {

    @Autowired
    private Gson gson;

    public static final Logger LOGGER = LoggerFactory.getLogger(DumpService.class);

    @Value("${regards.json.dump.max.per.sub.zip:1000}")
    private int MAX_FILES_PER_ZIP;

    private String folderPathPattern = "yyyy/MM/dd";

    @Value("${spring.application.name}")
    private String microservice;

    /**
     * Create a zip archive of multiple zips.
     * @param dumpCollection data to zip
     * @param zipLocation
     */
    public List<ObjectDump> generateJsonZips(List<ObjectDump> dumpCollection, String zipLocation) throws IOException {
        // check unique name
        List<ObjectDump> listErrorDumps = checkJsonNamesUnicity(dumpCollection);
        //remove duplicated entries in dumpCollection
        dumpCollection.removeAll(listErrorDumps);

        // Locals Vars
        String firstDate, lastDate, zipName;
        List<List<ObjectDump>> zipGlobal;
        ArrayList<String> subZipNameList = new ArrayList<>();
        int indexName, sizeCollection = dumpCollection.size();

        // Sort dump collection by date
        Collections.sort(dumpCollection);

        // Create datasets
        zipGlobal = createSets(dumpCollection, sizeCollection);

        // Create zip location if not existing
        Files.createDirectories(Paths.get(zipLocation));

        // Create Root Zip
        zipName = "dump_json_" + microservice + "_" + OffsetDateTimeAdapter.format(OffsetDateTime.now());
        try (ZipOutputStream rootZip = new ZipOutputStream(
                new FileOutputStream(new File(zipLocation + "/" + zipName + ".zip")));) {

            for (List<ObjectDump> dataSet : zipGlobal) {
                // Retrieve first and last date of set
                firstDate = OffsetDateTimeAdapter.format(dataSet.get(0).getCreationDate());
                lastDate = OffsetDateTimeAdapter.format(dataSet.get(dataSet.size() - 1).getCreationDate());
                zipName = firstDate + "_" + lastDate + ".zip";
                // Handle not unique zip names by adding index
                if (subZipNameList.contains(zipName)) {
                    indexName = 0;
                    do {
                        zipName = firstDate + "_" + lastDate + "_" + indexName + ".zip";
                        indexName++;
                    } while (subZipNameList.contains(zipName));
                }
                subZipNameList.add(zipName);

                // Add subzip to rootzip
                addSubZip(dataSet, rootZip, zipName, zipLocation);

            }
        }
        //return list of object not processed
        return listErrorDumps;
    }

    private List<List<ObjectDump>> createSets(List<ObjectDump> dumpCollection, int sizeCollection) {
        // Init
        List<List<ObjectDump>> zipGlobal = new ArrayList<>();
        List<ObjectDump> tmpObj = new ArrayList<>();
        int nbFilesPerSet = 0, indexList = 0;
        // Create set of objects to process with MAX_FILES_PER_ZIP
        for (ObjectDump objectDump : dumpCollection) {
            // Create object datasets
            tmpObj.add(objectDump);
            nbFilesPerSet++;
            if (nbFilesPerSet >= MAX_FILES_PER_ZIP || indexList == sizeCollection - 1) {
                zipGlobal.add(new ArrayList<>(tmpObj));
                tmpObj.clear();
                nbFilesPerSet = 0;
            }
            indexList++;
        }
        return zipGlobal;
    }

    private void addSubZip(List<ObjectDump> dataSet, ZipOutputStream rootZip, String zipName, String zipLocation) {
        // Create zips in global zip
        String filename, filePath, fileContent;
        DateTimeFormatter folderPathFormatter = DateTimeFormatter.ofPattern(folderPathPattern);
        ZipEntry zipEntry, fileEntry;
        try (ByteArrayOutputStream subZipByte = new ByteArrayOutputStream();
                ZipOutputStream subZip = new ZipOutputStream(new BufferedOutputStream(subZipByte));) {
            // For each set of files, create file in subZip
            for (ObjectDump file : dataSet) {
                filename = file.getJsonName() + ".json";
                filePath = folderPathFormatter.format(file.getCreationDate()) + "/" + filename;
                fileContent = this.gson.toJson(file.getJsonContent());
                // Add File to Sub Zip
                fileEntry = new ZipEntry(filePath);
                subZip.putNextEntry(fileEntry);
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
        } catch (IOException e) {
            LOGGER.error("Error while generating zips at {}", zipLocation, e);
        }
    }

    private List<ObjectDump> checkJsonNamesUnicity(List<ObjectDump> dumpCollection) {
        // init errorList
        List<ObjectDump> listErrorDumps = new ArrayList<>();
        // Create multimap with jsonNames
        ImmutableListMultimap<String, ObjectDump> dumpMultimap = Multimaps
                .index(dumpCollection, ObjectDump::getJsonName);
        // If duplicated keys were found, jsonNames are not uniques in collection
        if (dumpCollection.size() != dumpMultimap.keySet().size()) {
            dumpMultimap.asMap().forEach((key, collection) -> {
                if (collection.size() > 1) {
                    listErrorDumps.addAll(collection);
                }
            });
        }

        //return objectDumps with duplicated jsonNames
        return listErrorDumps;
    }

}

