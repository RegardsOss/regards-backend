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
package fr.cnes.regards.framework.utils.file.compression.tar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.utils.file.compression.AbstractRunnableCompression;
import fr.cnes.regards.framework.utils.file.compression.CompressManager;
import fr.cnes.regards.framework.utils.file.compression.CompressionException;
import fr.cnes.regards.framework.utils.file.compression.CompressionTypeEnum;
import fr.cnes.regards.framework.utils.file.compression.FileAlreadyExistException;

/**
 * Classe specialisee dans la compression de fichiers au format TAR. Elle prend en compte les particularites de l'outil
 * de compression TAR, principalement la technique d'appel.
 */
public class TarCompression extends AbstractRunnableCompression {

    private static Logger LOGGER = LoggerFactory.getLogger(TarCompression.class);

    /**
     * extension a ajouter
     */
    private static final String TAR_EXTENSION = ".tar";

    /**
     * Motif representant un fichier avec extension TAR modulo la casse.
     */
    private static final String TAR_PATTERN = ".*((\\.)(?i)tar)$";

    /**
     * Tampon d'ecriture
     */
    private static final int BUFFER = 1024;

    /**
     * Separateur dans path des fichiers tar, on n'utilise pas le seprarateur systeme pour le rendre independant d'une
     * plateforme, le seprarateur unix permettant une plus grande compatibilite.
     */
    private static final String TAR_PATH_SEPARATOR = "/";

    /**
     * Permet de compression une liste de fichiers dans un seul.
     * @param pFileList la liste de File a compresser
     * @param pCompressedFile définit le nom et le chemin du fichier compressé sans extension
     * @param pRootDirectory le répertoire racine de tous les fichiers à compresser.
     * @return le fichier compressé avec l'extension
     * @throws CompressionException si l'un des paramètres est incorrect ou illisible
     */
    @Override
    protected CompressManager runCompress(List<File> pFileList, File pCompressedFile, File pRootDirectory,
            boolean pFlatArchive, Charset pCharset, CompressManager pCompressManager) throws CompressionException {

        // if the file has no tar extension, we add one.
        final Pattern pat = Pattern.compile(TAR_PATTERN);
        File compressedFile = null;
        if (pat.matcher(pCompressedFile.getName()).matches()) {
            compressedFile = new File(pCompressedFile.getParentFile(), pCompressedFile.getName());
        } else {
            // Create a new file with .zip extension
            compressedFile = new File(pCompressedFile.getParentFile(), pCompressedFile.getName() + TAR_EXTENSION);
        }

        if (compressedFile.exists()) {
            throw new FileAlreadyExistException(String.format("File %s already exist", compressedFile.getName()),
                                                compressedFile);
        }

        long totalSize = 0;
        long percentage = 0;
        long compressedSize = 0;
        for (final File fileNow : pFileList) {
            totalSize += fileNow.length();
        }

        try (final FileOutputStream dest = new FileOutputStream(compressedFile)) {

            // Prepare streams
            try (TarArchiveOutputStream os = (TarArchiveOutputStream) new ArchiveStreamFactory()
                    .createArchiveOutputStream("tar", dest)) {

                // Allow long file names
                os.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

                for (final File fileNow : pFileList) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(String.format("Adding %s file to %s file.", fileNow.getName(),
                                                   CompressionTypeEnum.TAR.toString()));
                    }

                    TarArchiveEntry entry;
                    if (pFlatArchive) {
                        entry = new TarArchiveEntry(fileNow.getName());
                    } else {
                        entry = new TarArchiveEntry(convertSpecialCharacter(pathToRootDir(fileNow, pRootDirectory)));
                    }
                    entry.setModTime(fileNow.lastModified());
                    entry.setSize(fileNow.length());

                    try (final FileInputStream input = new FileInputStream(fileNow)) {

                        os.putArchiveEntry(entry);

                        final byte buffer[] = new byte[BUFFER];
                        int n = 0;
                        while (-1 != (n = input.read(buffer))) {
                            compressedSize += n;
                            os.write(buffer, 0, n);
                            percentage = (100 * compressedSize) / totalSize;
                            pCompressManager.setPercentage(percentage);
                        }

                        os.closeArchiveEntry();
                    }

                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("The file %s is done.", compressedFile.getAbsolutePath()));
                }
            }
        } catch (final IOException | ArchiveException ioE) {
            compressedFile.delete();
            LOGGER.error(ioE.getMessage(), ioE);
            throw new CompressionException(String.format("IO error during %s compression", CompressionTypeEnum.TAR),
                                           ioE);
        }

        pCompressManager.setCompressedFile(compressedFile);
        return pCompressManager;
    }

    /**
     * Permet de calculer le chemin relatif d'un fichier par rapport à un répertoire de plus haut niveau.
     * @param pFile le fichier
     * @param pRootDir le répertoire racine.
     * @return une chaine sous forme de <code>String</code> contenant le chemin relatif.
     */
    private String pathToRootDir(File pFile, File pRootDir) {

        File temporaryFile = new File(pFile.getAbsolutePath());

        final StringBuilder pathToRoot = new StringBuilder();
        pathToRoot.insert(0, temporaryFile.getName());

        boolean loop = !temporaryFile.getParentFile().getAbsolutePath().equals(pRootDir.getAbsolutePath());

        // @DM SIPNG-DM-0020-CN : we add / only if the path is not made only by the file name
        if (loop) {
            pathToRoot.insert(0, TAR_PATH_SEPARATOR);
        }

        while (loop) {
            pathToRoot.insert(0, temporaryFile.getParentFile().getName());

            temporaryFile = temporaryFile.getParentFile();

            loop = !temporaryFile.getParentFile().getAbsolutePath().equals(pRootDir.getAbsolutePath());
            if (loop) {
                // do not add file separator at the start of the path
                pathToRoot.insert(0, TAR_PATH_SEPARATOR);
            }
        }

        return pathToRoot.toString();
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

        // pCompressedFile must have .tar extension
        if (!pCompressedFile.getName().toLowerCase().endsWith(".tar")) {
            throw new CompressionException("Extension must be .tar");
        }

        // Write in this file
        TarArchiveInputStream inputStream = null;
        try (InputStream is = new FileInputStream(pCompressedFile)) {
            final int bufsize = 8192;
            TarArchiveEntry entry;
            if (pCharset != null) {
                inputStream = new TarArchiveInputStream(is, pCharset.toString());
            } else {
                inputStream = new TarArchiveInputStream(is);
            }
            if (!pOutputDir.exists()) {
                pOutputDir.mkdirs();
            }
            while (null != (entry = inputStream.getNextTarEntry())) {
                int bytesRead;
                if (entry.isDirectory()) {
                    final File fileOrDir = new File(pOutputDir, entry.getName());
                    fileOrDir.mkdir();
                    continue;
                }
                final byte[] buf = new byte[bufsize];
                final File file = new File(pOutputDir, entry.getName());
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    while ((bytesRead = inputStream.read(buf, 0, bufsize)) > -1) {
                        outputStream.write(buf, 0, bytesRead);
                    }
                } catch (final IOException | RuntimeException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("The file %s is uncompressed to %s", pCompressedFile.getName(),
                                           pOutputDir.getName()));
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (final IOException e) {
                throw new CompressionException(
                        String.format("IO error during %s uncompression", CompressionTypeEnum.TAR), e);
            }
        } catch (final IOException ioE) {
            throw new CompressionException(String.format("IO error during %s uncompression", CompressionTypeEnum.TAR),
                                           ioE);
        }
    }

    /**
     * Les accents et caractères spéciaux sont mal interprétés par l'API java.util.zip, il faut donc transformer les
     * noms qui en contiennent.
     * @param pString le nom de fichier a convertir
     * @return la chaine convertie
     */
    private String convertSpecialCharacter(String pString) {
        final String temp = Normalizer.normalize(pString, Form.NFD);
        return temp.replaceAll("[^\\p{ASCII}]", "");
    }

}