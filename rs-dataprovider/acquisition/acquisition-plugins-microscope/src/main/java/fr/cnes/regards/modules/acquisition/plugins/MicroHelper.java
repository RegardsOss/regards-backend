/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.plugins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import fr.cnes.regards.modules.acquisition.exception.MetadataException;

/**
 * Helper class
 * @author Olivier Rousselot
 */
public final class MicroHelper {

    private MicroHelper() {
    }

    /**
     * Return first tag value found somewhere into XML document
     * @throws MetadataException if tag is not present
     */
    public static String getTagValue(Document doc, String tag) throws MetadataException {
        NodeList elements = doc.getElementsByTagName(tag);
        if (elements.getLength() == 0) {
            throw new MetadataException("Cannot find tag '%s' into metadata XML file");
        }
        return elements.item(0).getTextContent();
    }

    /**
     * Data file is under sub-directory with same name as metadata xml file (after removing end "_metadata.xml").
     */
    public static File findDataFileIntoSubDir(Path metadataPath, String dataFilename) {
        String metadataFilename = metadataPath.getFileName().toString();
        Path dirPath = Paths.get(metadataPath.getParent().toString(),
                                 metadataFilename.substring(0, metadataFilename.indexOf(Microscope.METADATA_SUFFIX)));
        return Paths.get(dirPath.toString(), dataFilename).toFile();
    }

    /**
     * Data file is under same directory tree as metadata file except that metadata tree starts under "metadonnees"
     */
    public static File findDataFileIntoTreeDir(Path metadataPath, String dataFilename) {
        String metadataFilename = metadataPath.getFileName().toString();
        int rootPathIdx = -1;
        for (int i = 0; i < metadataPath.getNameCount(); i++) {
            if (metadataPath.getName(i).toString().equals(Microscope.HKTM_METADATA_ROOT_DIR)) {
                rootPathIdx = i;
                break;
            }
        }
        // Metadata file path does not contain METADATA_DIR (should not occur)
        if (rootPathIdx == -1) {
            return null;
        }
        return metadataPath.subpath(0, rootPathIdx)
                .resolve(metadataPath.subpath(rootPathIdx + 1, metadataPath.getNameCount() - 1))
                .resolve(metadataFilename.substring(0, metadataFilename.indexOf(Microscope.METADATA_SUFFIX)))
                .resolve(dataFilename).toFile();
    }

    /**
     * Return first file found with extension into given directory
     */
    public static File findFileWithExtension(File dir, String ext) throws FileNotFoundException {
        File[] files = dir.listFiles(pathname -> pathname.getName().endsWith(ext));
        if (files.length == 0) {
            throw new FileNotFoundException(
                    String.format("No file with extension '%s' into '%s' directory", ext, dir.getAbsolutePath()));
        }
        // Return first one
        return files[0];
    }

    /**
     * Return sub-directory with same name as metada file (after removing "_metadata.xml" end)
     */
    public static File findDirStartingWithSameName(Path metadataPath) throws IOException {
        String metadataFilename = metadataPath.getFileName().toString();
        File dir = Paths.get(metadataPath.getParent().toString(),
                             metadataFilename.substring(0, metadataFilename.indexOf(Microscope.METADATA_SUFFIX)))
                .toFile();
        if (!dir.exists()) {
            throw new FileNotFoundException(
                    String.format("Directory '%s' does not exist", dir.getAbsolutePath()));
        } else if (!dir.isDirectory()) {
            throw new IOException(String.format("'%s' isn't a directory", dir.getAbsolutePath()));
        }
        return dir;
    }
}
