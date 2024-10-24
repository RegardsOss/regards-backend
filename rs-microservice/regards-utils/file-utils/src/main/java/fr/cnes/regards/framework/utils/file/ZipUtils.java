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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utils for zip archive generation and extraction methods
 *
 * @author Thibaud Michaudel
 **/
public class ZipUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipUtils.class);

    /**
     * Create a flat zip archive containing the listed files.
     * The archive is deleted if an error occur during its creation.
     *
     * @param archive   the zip archive that will be created
     * @param filesList the files that will be added to the archives
     * @return true if the archive was successfully created
     */
    public static boolean createZipArchive(File archive, List<File> filesList) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(archive)) {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
                for (File file : filesList) {
                    boolean success = addFileToArchive(zipOutputStream, file);
                    if (!success) {
                        Files.delete(archive.toPath());
                        return false;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error while creating archive {}", archive.getName(), e);
            try {
                Files.delete(archive.toPath());
            } catch (IOException ex) {
                LOGGER.error("Error while attempting to delete partially created archive {} following error",
                             archive.getName(),
                             e);
            }
            return false;
        }
        return true;
    }

    /**
     * Extract all the files from an archive
     *
     * @param archive     the archive to extract from
     * @param destination the path where the extracted file will be copied
     * @return true if the unzipping succeeded
     */
    public static boolean unzip(Path archive, Path destination) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archive.toFile()))) {
            byte[] buffer = new byte[1024];
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(destination.toFile(), zipEntry);
                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        LOGGER.error("Failed to create directory  {}", newFile);
                        return false;
                    }
                } else {
                    // fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        LOGGER.error("Failed to create directory  {}", parent);
                        return false;
                    }

                    // write file content
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }

            zis.closeEntry();
        } catch (IOException e) {
            LOGGER.error("Error while unzipping archive {}", archive, e);
            return false;
        }
        return true;
    }

    /**
     * Extract one file from the archive
     *
     * @param archive     the archive to extract from
     * @param fileName    the file to extract
     * @param destination the path where the extracted file will be copied
     */
    public static void extractFile(Path archive, String fileName, Path destination) throws IOException {
        try (FileSystem fileSystem = FileSystems.newFileSystem(archive)) {
            Path fileToExtract = fileSystem.getPath(fileName);
            Files.createDirectories(destination);
            Files.copy(fileToExtract, destination.resolve(fileName));
        }
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    private static boolean addFileToArchive(ZipOutputStream zipOutputStream, File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zipOutputStream.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOutputStream.write(bytes, 0, length);
            }
            return true;
        } catch (IOException e) {
            LOGGER.error("Error while adding file {} to archive", file.getName(), e);
            return false;
        }
    }

    /**
     * Add filse to the given zip stream
     */
    public static boolean addFilesToArchive(ZipOutputStream zipOutputStream, List<File> files) {
        for (File file : files) {
            if (!addFileToArchive(zipOutputStream, file)) {
                return false;
            }
        }
        return true;
    }
}
