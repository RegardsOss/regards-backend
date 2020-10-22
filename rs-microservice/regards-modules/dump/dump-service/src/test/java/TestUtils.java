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

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.modules.dump.service.ObjectDump;

/**
 *
 * @author Iliana Ghazali
 */

public class TestUtils {

    public static String checkZipCreation(File parentZip, List<ObjectDump> zipCollection) {
        // Init vars
        int validNbFiles = zipCollection.size();
        String zipName = parentZip.getName();

        // Verify name of zip
        String firstDate = OffsetDateTimeAdapter.format(zipCollection.get(0).getCreationDate());
        String lastDate = OffsetDateTimeAdapter.format(zipCollection.get(validNbFiles - 1).getCreationDate());
        String validSubZipName = firstDate + "_" + lastDate + "(_[0-9]+)?\\.zip$";
        if (!zipName.matches(validSubZipName)) {
            return "The name of the created subzip \"" + zipName + "\" does not match the expected format : "
                    + validSubZipName;
        }

        // Verify number of created files in zip
        List<String> fileNames = readZipEntryNames(parentZip);
        int createdNbFiles = fileNames.size();
        if (validNbFiles != createdNbFiles) {
            return "The number of json files in zip \"" + zipName + " is unexpected, " + createdNbFiles
                    + " were created instead of " + validNbFiles;
        }

        // Verify created paths
        int indexFile = 0;
        String validPath, createdPath;
        ObjectDump validObjectDump;
        DateTimeFormatter pathFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        while (indexFile < validNbFiles) {
            validObjectDump = zipCollection.get(indexFile);
            validPath = validObjectDump.getCreationDate().format(pathFormatter) + "/" + validObjectDump.getJsonName()
                    + ".json";
            createdPath = fileNames.get(indexFile);
            if (!validPath.equals(createdPath)) {
                return "Expected path \"" + validPath + "\", found path \"" + createdPath + "\" instead";
            }
            indexFile++;
        }
        return "";
    }

    public static List<String> readZipEntryNames(File parentZip) {
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
}