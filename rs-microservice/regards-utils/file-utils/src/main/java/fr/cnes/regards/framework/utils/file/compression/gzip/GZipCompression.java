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
package fr.cnes.regards.framework.utils.file.compression.gzip;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.utils.file.compression.AbstractRunnableCompression;
import fr.cnes.regards.framework.utils.file.compression.CompressManager;
import fr.cnes.regards.framework.utils.file.compression.CompressionException;
import fr.cnes.regards.framework.utils.file.compression.CompressionTypeEnum;
import fr.cnes.regards.framework.utils.file.compression.FileAlreadyExistException;
import fr.cnes.regards.framework.utils.file.compression.tar.TarCompression;

/**
 * Classe specialisee dans la compression de fichiers au format GZIP. Elle prend en compte les particularites de l'outil
 * de compression GZIP. L'algorithme GZIP ne peut compresser qu'un seul fichier. C'est pourquoi cette classe va utiliser
 * la concatenation TAR dans un premier temps pour rassembler les fichiers en un seul puis la compression GZIP. S'il n'y
 * a qu'un fichier a compresser il est directement compressé en GZIP
 * @author CS
 */
public class GZipCompression extends AbstractRunnableCompression {

    /**
     * Extension tar
     */
    private static final String TAR_EXTENSION = ".tar";

    /**
     * Extension GZIP
     */
    private static final String GZIP_EXTENSION = ".gz";

    /**
     * Taille de l'extension GZIP
     */
    private static final int GZIP_EXTENSION_LENGTH = 3;

    /**
     * Motif representant un fichier avec extension GZIP modulo la casse.
     */
    private static final String GZIP_PATTERN = ".*((\\.)(?i)gz)$";

    /**
     * Motif representant une extension GZIP modulo la casse et le point.
     */
    private static final String SINGLE_GZIP_PATTERN = "((\\.)?(?i)gz)$";

    /**
     * Cette variable est utilisée pour logger les messages
     */
    private static Logger logger = LoggerFactory.getLogger(GZipCompression.class);

    /**
     * Tampon d'ecriture
     */
    private static final int BUFFER = 1024;

    /**
     * Permet de compression une liste de fichiers dans un seul.
     * @param pFileList la liste de File a compresser
     * @param pCompressedFile définit le nom et le chemin du fichier compresse.
     * @param pRootDirectory le répertoire racine de tous les fichiers à compresser.
     * @return le fichier compressé avec l'extension
     * @throws CompressionException si l'un des paramètres est incorrect ou illisible
     */
    @Override
    public CompressManager runCompress(List<File> pFileList, File pCompressedFile, File pRootDirectory,
            boolean pFlatArchive, Charset pCharset, CompressManager pCompressManager) throws CompressionException {
        File returnedFile = null;
        File compressedTarFile = null;

        // if the file has a gzip extension, we remove it before taring one.
        Pattern pat = Pattern.compile(GZIP_PATTERN);
        File compressedFile = null;
        // in case of file such as file.tar.gz , transform in file.tar
        if (pat.matcher(pCompressedFile.getName()).matches()) {
            pat = Pattern.compile(SINGLE_GZIP_PATTERN);
            Matcher match = pat.matcher(pCompressedFile.getName());
            compressedFile = new File(pCompressedFile.getParentFile(), match.replaceAll(""));
        } else {
            compressedFile = new File(pCompressedFile.getParentFile(), pCompressedFile.getName());
        }

        TarCompression tar = new TarCompression();
        // Run in thread ?
        if (pCompressManager.getThread() != null) {
            pCompressManager.setRatio(0.25);
            tar.compress(pFileList, compressedFile, pRootDirectory, pFlatArchive, pCharset, pCompressManager);
            // In asycnhrone mode only one result file is possible
            compressedTarFile = pCompressManager.getCompressedFile();
            pCompressManager.setRatio(0.75);
        } else {
            CompressManager compressManager = tar
                    .compress(pFileList, compressedFile, pRootDirectory, pFlatArchive, Boolean.FALSE);
            compressedTarFile = compressManager.getCompressedFile();
        }

        returnedFile = compressOneFile(compressedTarFile, compressedTarFile, pCompressManager);
        compressedTarFile.delete();

        pCompressManager.setRatio(1);
        pCompressManager.setPercentage(100);

        pCompressManager.setCompressedFile(returnedFile);
        return pCompressManager;
    }

    /**
     * Permet de decompresser un fichier compresse dans un repertoire cible
     * @param pCompressedFile le fichier a decompresser
     * @param pOutputDir le repertoire destination
     * @throws CompressionException si l'un des paramètres est incorrect ou illisible
     */
    @Override
    public void uncompress(File pCompressedFile, File pOutputDir) throws CompressionException {
        uncompress(pCompressedFile, pOutputDir, null);
    }

    /**
     * Permet de decompresser un fichier compresse dans un repertoire cible
     * @param pCompressedFile le fichier a decompresser
     * @param pOutputDir le repertoire destination
     * @throws CompressionException si l'un des paramètres est incorrect ou illisible
     */
    @Override
    public void uncompress(File pCompressedFile, File pOutputDir, Charset pCharset) throws CompressionException {

        File tmpFile = uncompressOneFile(pCompressedFile, pOutputDir);

        // If GZIP file contains a TAR file, then it will be deflated
        // and the TAR file is deleted
        if (tmpFile.getName().toLowerCase().endsWith(TAR_EXTENSION)) {
            TarCompression tar = new TarCompression();
            tar.uncompress(tmpFile, pOutputDir);
            tmpFile.delete();
        }

    }

    /**
     * Méthode utilisée pour compresser un unique fichier puisque l'algorithme GZIP ne peut directement en compresser
     * plusieurs
     * @param pFileToCompress le fichier a compresser
     * @param pCompressedFile définit le nom et le chemin du fichier compressé sans extension
     * @return le fichier compressé avec l'extension
     * @throws {@link CompressionException}
     * @since 1.0
     */
    private File compressOneFile(File pFileToCompress, File pCompressedFile, CompressManager pCompressManager)
            throws CompressionException {

        if (logger.isDebugEnabled()) {
            logger.debug("Add " + pFileToCompress.getName() + " to GZIP file.");
        }

        long totalSize = pFileToCompress.length();
        long percentage = 0;
        long compressedSize = 0;

        // if the file has no gzip extension, we add one.
        Pattern pat = Pattern.compile(GZIP_PATTERN);
        File compressedFile = null;
        if (pat.matcher(pCompressedFile.getName()).matches()) {
            compressedFile = new File(pCompressedFile.getParentFile(), pCompressedFile.getName());
        } else {
            // Create a new file with .zip extension
            compressedFile = new File(pCompressedFile.getParentFile(), pCompressedFile.getName() + GZIP_EXTENSION);
        }

        if (compressedFile.exists()) {
            throw new FileAlreadyExistException(String.format("File %s already exist", compressedFile.getName()),
                                                compressedFile);
        }

        // Write in this file
        try (GZIPOutputStream out = new GZIPOutputStream(new BufferedOutputStream(
                new CheckedOutputStream(new FileOutputStream(compressedFile), new Adler32())))) {
            byte data[] = new byte[BUFFER];
            try (BufferedInputStream origin = new BufferedInputStream(new FileInputStream(pFileToCompress), BUFFER)) {
                int count = 0;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    compressedSize += count;
                    out.write(data, 0, count);
                    percentage = (100 * compressedSize) / totalSize;
                    if (percentage > 0) {
                        pCompressManager.upPercentage(percentage);
                        compressedSize = 0;
                    }
                }
            }
        } catch (IOException ioE) {
            logger.error(ioE.getMessage(), ioE);
            throw new CompressionException(String.format("IO error during %s compression", CompressionTypeEnum.GZIP),
                                           ioE);
        }
        return compressedFile;
    }

    /**
     * Méthode utilisée pour compresser un unique fichier puisque l'algorithme GZIP ne peut directement en compresser
     * plusieurs
     * @param pCompressedFile le fichier a decompresser
     * @param pOutputDir le repertoire où decompresser le fichier
     * @return le fichier decompresse
     * @throws {@link CompressionException}
     */
    private File uncompressOneFile(File pCompressedFile, File pOutputDir) throws CompressionException {

        // pCompressedFile must have .gz extension
        if (!pCompressedFile.getName().toLowerCase().endsWith(GZIP_EXTENSION)) {
            String msg = String.format("Extension must be %s", GZIP_EXTENSION);
            logger.error(msg);
            throw new CompressionException(msg);
        }

        // Create uncompressedFile
        File returnedFile = new File(pOutputDir, pCompressedFile.getName()
                .substring(0, pCompressedFile.getName().length() - GZIP_EXTENSION_LENGTH));
        if (returnedFile.exists()) {
            String msg = String.format("File %s already exist", returnedFile.getName());
            logger.error(msg);
            throw new FileAlreadyExistException(msg);
        }

        // Write in this file
        try (GZIPInputStream zis = new GZIPInputStream(new BufferedInputStream(new FileInputStream(pCompressedFile)))) {
            byte data[] = new byte[BUFFER];

            try (BufferedOutputStream dest = new BufferedOutputStream(new FileOutputStream(returnedFile), BUFFER)) {
                int count = 0;
                while ((count = zis.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
            }
        } catch (IOException ioE) {
            throw new CompressionException(String.format("IO error during %s uncompression", CompressionTypeEnum.GZIP),
                                           ioE);
        }
        return returnedFile;
    }

}
