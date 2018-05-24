/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Utils to cut files into multiple files.
 *
 * @author sbinda
 *
 */
public final class CutFileUtils {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(CutFileUtils.class);

    /**
     * Inputstream buffer reader size
     */
    private static int BUFFER_SIZE = 1024;

    private CutFileUtils() {

    }

    /**
     * Cut the give {@link File} pFile into parts of maximum pCutfilesMaxSize into pTargetDirectory
     * @param pFileToCut {@link File} to cut
     * @param pTargetDirectory Target directory to create cut files.
     * @param pCutFileNamesPrefix cut file are named with this prefix and "_<part index>".
     * @param pCutfilesMaxSize Max size of each cuted file.
     * @return {@link Set} of cut {@link File}
     * @throws IOException I/O exception during file cuting.
     */
    public static Set<File> cutFile(File pFileToCut, String pTargetDirectory, String pCutFileNamesPrefix,
            long pCutfilesMaxSize) throws IOException {

        Set<File> cutFiles = Sets.newHashSet();
        try (FileInputStream inputStream = new FileInputStream(pFileToCut)) {
            int fileCount = 0;
            boolean continueCutFile = true;
            do {
                // New cut file to write
                String strFileCount = StringUtils.leftPad(String.valueOf(fileCount), 2, "0");
                LOG.debug("creating new cut File {}_{}", pCutFileNamesPrefix, strFileCount);
                File cutFile = new File(pTargetDirectory, pCutFileNamesPrefix + "_" + strFileCount);
                continueCutFile = writeInFile(inputStream, cutFile, pCutfilesMaxSize);
                cutFiles.add(cutFile);
                fileCount++;

            } while (continueCutFile);

        } catch (IOException e) {
            String msg = "Error cutting file" + pFileToCut + " to directory " + pTargetDirectory;
            LOG.error(msg, e);
            throw new IOException(msg, e);
        }
        return cutFiles;
    }

    /**
     * Write from a given {@link FileInputStream} to the output given {@link File} a maximum of <maxSizeToWrite> bytes.
     * @param pInputStream {@link FileInputStream} reader
     * @param outputFile {@link File} to write to
     * @param maxSizeToWrite maximum number of bytes to write
     * @return TRUE if there is more bytes to read from pInputStream after writing the maximum number of bytes.
     * @throws IOException I/O exception.
     */
    private static boolean writeInFile(FileInputStream pInputStream, File outputFile, Long maxSizeToWrite)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        if (maxSizeToWrite < BUFFER_SIZE) {
            buffer = new byte[maxSizeToWrite.intValue()];
        }
        long charcount = 0;
        int charRead = 0;
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            while ((charcount < maxSizeToWrite) && (charRead >= 0)) {
                charRead = pInputStream.read(buffer);
                if (charRead >= 0) {
                    charcount = charcount + charRead;
                    outputStream.write(buffer, 0, charRead);
                }
            }
            outputStream.flush();
        }
        return charRead > 0;
    }

    /**
     * Rebuild a cuted file by concatenation of the part files
     * @param pFilePathToRebuild {@link Path} to the result file
     * @param pOrderedPartFilePaths {@link SortedSet} of {@link Path}s of part files. <br/>
     * The set have to be ordered from the first part of the result file to the last one.
     * @throws IOException
     */
    public static void rebuildCutedfile(Path pFilePathToRebuild, SortedSet<Path> pOrderedPartFilePaths)
            throws IOException {

        // 1. Check if result file already exists
        if (Files.exists(pFilePathToRebuild)) {
            throw new FileAlreadyExistsException(
                    String.format("Error rebuilding cuted file. Destination file %s already exists",
                                  pFilePathToRebuild.toString()));
        }
        Files.createFile(pFilePathToRebuild);
        try (FileOutputStream os = new FileOutputStream(pFilePathToRebuild.toFile(), true)) {
            for (Path partFilePath : pOrderedPartFilePaths) {
                LOG.debug("Adding part file {} into {}", partFilePath.toString(), pFilePathToRebuild.toString());
                writeFile(os, partFilePath);
            }
            os.flush();
        }
    }

    /**
     * Read and write the given {@link Path} file to the given {@link FileOutputStream}
     * @param pOutputStream {@link FileOutputStream} of the result.
     * @param filePathToWrite {@link Path} of the file to read and write.
     * @throws IOException
     */
    private static void writeFile(FileOutputStream pOutputStream, Path filePathToWrite) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int charRead = 0;
        try (FileInputStream is = new FileInputStream(filePathToWrite.toFile())) {
            do {
                charRead = is.read(buffer);
                if (charRead >= 0) {
                    pOutputStream.write(buffer, 0, charRead);
                }
            } while (charRead >= 0);
        }
    }

}
