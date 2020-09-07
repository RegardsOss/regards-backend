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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;

/**
 *
 * @author Iliana Ghazali
 */

public class TestUtils {

    public static List<List<ObjectDump>> createSets(List<ObjectDump> dumpCollection, int sizeCollection,
            int maxFilesPerSubZip) {
        // Init
        List<List<ObjectDump>> zipGlobal = new ArrayList<>();
        List<ObjectDump> tmpObj = new ArrayList<>();
        int nbFilesPerSet = 0, indexList = 0;

        // Sort collection
        Collections.sort(dumpCollection);

        // Create set of objects to process with MAX_FILES_PER_ZIP
        for (ObjectDump objectDump : dumpCollection) {
            // Create object datasets
            tmpObj.add(objectDump);
            nbFilesPerSet++;
            if (nbFilesPerSet >= maxFilesPerSubZip || indexList == sizeCollection - 1) {
                zipGlobal.add(new ArrayList<>(tmpObj));
                tmpObj.clear();
                nbFilesPerSet = 0;
            }
            indexList++;
        }
        return zipGlobal;
    }

    public static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    public static Map<String, List<ZipEntry>> readZipEntries(File parentFileZip) {
        Map<String, List<ZipEntry>> mapZipEntries = new LinkedHashMap<>();
        ZipEntry fileEntry, subZipEntry;
        String subZipName;

        try (ZipFile parentZip = new ZipFile(parentFileZip.getPath())) {
            Enumeration<? extends ZipEntry> subZipsEntries = parentZip.entries();

            while (subZipsEntries.hasMoreElements()) {
                subZipEntry = subZipsEntries.nextElement();
                subZipName = subZipEntry.getName();
                ZipInputStream subZipInputStream = new ZipInputStream(parentZip.getInputStream(subZipEntry));
                if (!mapZipEntries.containsKey(subZipName)) {
                    mapZipEntries.put(subZipName, new LinkedList<>());
                }
                while ((fileEntry = subZipInputStream.getNextEntry()) != null) {
                    mapZipEntries.get(subZipName).add(fileEntry);
                }

                subZipInputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mapZipEntries;
    }

    /**
     * Check the creation of the zip
     * @param parentFileZip folder that contains the zip
     * @return potential error
     */
    public static String checkZipCreation(File parentFileZip) {
        String errorMsg = "";
        // expected pattern
        String dateRegex = "((?:[1-9][0-9]*)?[0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])\\.([0-9]{1,6})Z";
        String microserviceProperty = "test";
        String zipNameRegex = "^dump_json_" + microserviceProperty + "_" + dateRegex + "\\.zip$";
        // check zip name
        String zipName = parentFileZip.getName();
        if (!zipName.matches(zipNameRegex)) {
            errorMsg = "the name of the created zip \"" + zipName
                    + "\" is incorrect and should match the following pattern : dump_json_microservice_dumpdate ("
                    + zipNameRegex + ")";
        }
        return errorMsg;
    }

    /**
     * Check if the creation of subzips inside a global zip is as expected
     * @param parentFileZip global zip
     * @param sets sets of object dump
     * @return list of potential errors
     */
    public static LinkedList<String> checkSubZipCreation(File parentFileZip, List<List<ObjectDump>> sets) {

        boolean flagError = false;
        LinkedList<String> errorReasons = new LinkedList<>();
        Map<String, List<ZipEntry>> mapSubZips = readZipEntries(parentFileZip);

        // Check the number of created zips in the global zip
        if (sets.size() != mapSubZips.size()) {
            return new LinkedList<>(Collections.singleton("The number of created subzips is unexpected (for example : "
                                                                  + "the json files were not grouped by regards.json.dump.max.per.sub.zip "
                                                                  + "or not all json were dumped"));
        }

        // Local vars
        String firstDate, lastDate;
        String validSubZipName, validPath;
        DateTimeFormatter pathFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

        int nbSubZips = sets.size(), validNbFiles, createdNbFiles;
        int indexSubZip = 0, indexFile = 0;

        List<ObjectDump> validSubZip;
        ObjectDump validFile;

        List<ZipEntry> createdSubZip;
        String createdSubZipName;
        LinkedList<String> createdSubZipNames = new LinkedList<>(mapSubZips.keySet());

        // Verifications
        while (indexSubZip < nbSubZips && !flagError) {
            validSubZip = sets.get(indexSubZip);
            createdSubZipName = createdSubZipNames.get(indexSubZip);
            createdSubZip = mapSubZips.get(createdSubZipName);

            // Verify number of created files in a subZip
            validNbFiles = validSubZip.size();
            createdNbFiles = createdSubZip.size();
            if (validNbFiles != createdNbFiles) {
                errorReasons.add("The number of json files in subzip \"" + createdSubZipName + " is unexpected, "
                                         + createdNbFiles + " were created instead of " + validNbFiles);
                break;
            }

            // Verify name of subZip
            firstDate = OffsetDateTimeAdapter.format(validSubZip.get(0).getCreationDate());
            lastDate = OffsetDateTimeAdapter.format(validSubZip.get(validNbFiles - 1).getCreationDate());
            validSubZipName = firstDate + "_" + lastDate + "(_[0-9]+)?\\.zip$";

            if (!createdSubZipNames.get(indexSubZip).matches(validSubZipName)) {
                errorReasons.add("The name of the created subzip \"" + createdSubZipNames.get(indexSubZip)
                                         + "\" does not match the expected format : " + validSubZipName);
                break;
            }

            // Verify created paths
            while (indexFile < validNbFiles && !flagError) {
                validFile = validSubZip.get(indexFile);
                validPath = validFile.getCreationDate().format(pathFormatter) + "/" + validFile.getJsonName() + ".json";
                if (!validPath.equals(createdSubZip.get(indexFile).getName())) {
                    flagError = true;
                    errorReasons.add("Expected path \"" + validPath + "\", found path \"" + createdSubZip.get(indexFile)
                            .getName() + "\" instead");
                }
                indexFile++;
            }
            indexFile = 0;
            indexSubZip++;
        }
        return errorReasons;
    }
}
