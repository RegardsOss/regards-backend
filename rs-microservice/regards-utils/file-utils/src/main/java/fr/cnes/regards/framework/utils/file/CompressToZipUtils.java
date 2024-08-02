/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.file;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Utility to compress files to a zip recursively.
 *
 * @author Iliana Ghazali
 **/
public final class CompressToZipUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompressToZipUtils.class);

    private static final long LARGE_FILE_SIZE_COPY_LIMIT_IN_BYTES = 2_000_000_000L; // 2GB

    private CompressToZipUtils() {
        // utils class
    }

    /**
     * Create a zip from files recursively. The zip destination must not have the same parent as the
     * source directory.
     *
     * @param sourceDirectoryPath directory where files are contained in root or subdirectories.
     * @param destZipPath         where to write the zip
     * @throws IOException if zip could not be created
     */
    public static void compressDirectoriesToZip(Path sourceDirectoryPath, Path destZipPath) throws IOException {
        long start = System.currentTimeMillis();
        LOGGER.debug("Creating zip at '{}' from files located at '{}'.", destZipPath, sourceDirectoryPath);
        // check if zip is not in contained in source directory
        if (destZipPath.getParent().equals(sourceDirectoryPath)) {
            throw new IOException(String.format("""
                                                    Zip destination path must not be contained in the source directory with files to zip:
                                                     - zip path: '%s'
                                                     - files source path: '%s'.""", destZipPath, sourceDirectoryPath));
        }
        // zip files
        try (FileOutputStream fileOutputStream = new FileOutputStream(destZipPath.toFile());
            ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(fileOutputStream)) {
            addFileToZipArchive(sourceDirectoryPath.toFile(), Path.of("/"), zipOutputStream);
        }
        LOGGER.debug("Successfully created zip at '{}'. Took {}ms.", destZipPath, System.currentTimeMillis() - start);
    }

    /**
     * Recursively add files to zip into a zip stream.
     *
     * @param sourceFile      directory that contains at the end the file to zip
     * @param relativePath    path leading at the end to the file to zip
     * @param zipOutputStream stream to build zip
     * @throws IOException if an error occurred during the zip construction.
     */
    private static void addFileToZipArchive(File sourceFile, Path relativePath, ZipArchiveOutputStream zipOutputStream)
        throws IOException {
        File[] files = sourceFile.listFiles();
        if (files != null) {
            for (File file : files) {
                Path relativeFilePath = relativePath.resolve(file.getName());
                if (file.isDirectory()) {
                    // Recursively find file to compress
                    addFileToZipArchive(file, relativeFilePath, zipOutputStream);
                } else {
                    // Create a zip entry
                    long fileSize = file.length();
                    LOGGER.trace("Found file at '{}' with size of {} bytes.", file.getAbsolutePath(), fileSize);
                    ZipArchiveEntry archiveEntry = new ZipArchiveEntry(relativeFilePath.toString());
                    archiveEntry.setSize(fileSize);
                    zipOutputStream.putArchiveEntry(archiveEntry);
                    copyFileStream(zipOutputStream, file, fileSize);
                    zipOutputStream.closeArchiveEntry();
                }
            }
        }
    }

    /**
     * Copy bytes from the file input stream to the zip output stream by taking into account its file size.
     * A file is considered as large if its size exceeds {@link #LARGE_FILE_SIZE_COPY_LIMIT_IN_BYTES}.
     */
    private static void copyFileStream(ZipArchiveOutputStream zipOutputStream, File file, long fileSize)
        throws IOException {
        // copy file to zip
        long bytesCopied;
        if (fileSize < LARGE_FILE_SIZE_COPY_LIMIT_IN_BYTES) {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                bytesCopied = IOUtils.copy(fileInputStream, zipOutputStream);
            }
        } else {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                bytesCopied = IOUtils.copyLarge(fileInputStream, zipOutputStream);
            }
        }
        // verify file was copied entirely
        if (bytesCopied != fileSize) {
            throw new IOException(String.format(
                "Unexpected number of bytes copied for file '%s'. Expected %d bytes but was %d bytes.",
                file.getAbsolutePath(),
                fileSize,
                bytesCopied));
        }
    }

}
