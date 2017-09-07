/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.file.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Utils to cut files into multiple files.
 *
 * @author sbinda
 *
 */
public class CutFileUtils {

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
     * @param pCutfilesMaxSize Max size of each cuted file.
     * @return {@link Set} of cut {@link File}
     * @throws IOException I/O exception during file cuting.
     */
    public static Set<File> cutFile(File pFileToCut, String pTargetDirectory, long pCutfilesMaxSize)
            throws IOException {

        Set<File> cutFiles = Sets.newHashSet();
        try (FileInputStream inputStream = new FileInputStream(pFileToCut)) {
            int fileCount = 0;
            boolean continueCutFile = true;
            do {
                // New cut file to write
                String strFileCount = StringUtils.leftPad(String.valueOf(fileCount), 2, "0");
                LOG.info("creating new cut File " + pFileToCut.getName() + "_" + strFileCount);
                File cutFile = new File(pTargetDirectory, pFileToCut.getName() + "_" + strFileCount);
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

}
