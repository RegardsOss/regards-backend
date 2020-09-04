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
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
        ZipFile parentZip;
        ZipEntry fileEntry, subZipEntry;
        String subZipName;

        try {
            parentZip = new ZipFile(parentFileZip.getPath());
            Enumeration<? extends ZipEntry> subZipsEntries = parentZip.entries();

            while (subZipsEntries.hasMoreElements()) {
                subZipEntry = subZipsEntries.nextElement();
                subZipName = subZipEntry.getName();
                ZipInputStream subZipInputStream = new ZipInputStream(parentZip.getInputStream(subZipEntry));
                while ((fileEntry = subZipInputStream.getNextEntry()) != null) {
                    if (!mapZipEntries.containsKey(subZipName)) {
                        mapZipEntries.put(subZipName, new LinkedList<>());
                    }
                    mapZipEntries.get(subZipName).add(fileEntry);
                }

                subZipInputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mapZipEntries;
    }
}
