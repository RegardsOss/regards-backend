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
package fr.cnes.regards.modules.acquisition.tools.compression;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.tools.compression.exception.CompressionException;
import fr.cnes.regards.modules.acquisition.tools.compression.exception.FileAlreadyExistException;
import fr.cnes.regards.modules.acquisition.tools.compression.zip.ZipCompression;

/**
 * 
 * Cette classe est la facade du paquetage de compression. La Facade cree une instance de Compression, et la passe au
 * contexte de compression qui s'occupe de l'execution. Les informations necessaires a la compression sont gerees au
 * niveau du contexte "CompressionContext".
 * 
 * @author Christophe Mertz
 */
public class CompressionFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompressionFacade.class);

    private static final int BYTES_IN_KILOBYTE = 1024;

    /**
     * Le contexte de compression qui pilote la compression proprement dite
     */
    private final CompressionContext strategy;

    //    private int maxFileSize_ = 2000000;

    /**
     * Constructeur par defaut
     */
    public CompressionFacade() {
        strategy = new CompressionContext();
    }

    /**
     * Taille max des fichiers compresses en ko : 2Go valeur par défaut
     */
    private long maxArchiveSize = 2000000;

    /**
     * Compression d'une liste de fichiers
     * 
     * @param pFileList
     *            une liste contenant les fichiers a compresser (classe File)
     * @param zipFile
     *            definit le chemin et le nom du fichier compresse SANS l'extension
     * 
     * @return le fichier compresse
     */
    private CompressManager compress(List<File> fileList, File zipFile) throws CompressionException {

        // specify input file list
        strategy.setInputSource(fileList);

        // specify compressed file
        strategy.setCompressedFile(zipFile);

        CompressManager manager = strategy.doCompress();

        if (!strategy.isRunInThread() && manager.getCompressedFile() != null
                && manager.getCompressedFile().length() == 0) {
            throw new CompressionException(String.format("A compressed file is required"));
        }

        return manager;
    }

    /**
     * Cette methode permet de compresser un fichier ou ensemble de fichier d'un repertoire vers un autre repertoire La
     * liste des fichiers a compresser est precisee, mais si le parametre pFileList est nul tout le repertoire en entree
     * est compresse.
     * 
     * @param compressionMode
     *            le type de compression (ZIP, GZIP, TAR, ...)
     * @param inputDirectory
     *            repertoire source
     * @param fileList
     *            une liste contenant les fichiers a compresser (classe File)
     * @param zipFile
     *            definit le chemin et le nom du fichier compresse SANS l'extension
     * @param rootDirectory
     *            le répertoire racine dans le cas d'une liste de fichiers.
     * @param flatArchive
     *            contenu de l'archive à plat ou non
     * 
     * @return la liste des fichiers compresses
     * 
     * @throws CompressionException
     *             si l'un des paramètres n'est pas correct
     */
    public Vector<CompressManager> compress(CompressionTypeEnum compressionMode, File inputDirectory, List<File> fileList,
            File zipFile, File rootDirectory, Boolean flatArchive, Boolean runInThread)
            throws CompressionException {

        return compress(compressionMode, inputDirectory, fileList, zipFile, rootDirectory, flatArchive, runInThread, null);

    }

    /**
     * Cette methode permet de compresser un fichier ou ensemble de fichier d'un repertoire vers un autre repertoire La
     * liste des fichiers a compresser est precisee, mais si le parametre pFileList est nul tout le repertoire en entree
     * est compresse.
     * 
     * Cette methode permet de définir l'encodage utilisé pour la compression.
     * 
     * Attention : l'encodage des caractères n'est implémenté que pour le format ZIP.
     * 
     * @param compressionMode
     *            le type de compression (ZIP, GZIP, TAR, ...)
     * @param inputDirectory
     *            repertoire source
     * @param fileList
     *            une liste contenant les fichiers a compresser (classe File)
     * @param zipFile
     *            definit le chemin et le nom du fichier compresse SANS l'extension
     * @param rootDirectory
     *            le répertoire racine dans le cas d'une liste de fichiers.
     * @param flatArchive
     *            contenu de l'archive à plat ou non
     * @param aCharset
     *            Encodage des caractères utilisé lors de la compression.
     * 
     * @return la liste des fichiers compresses
     * 
     * @throws CompressionException
     *             si l'un des paramètres n'est pas correct
     */
    public Vector<CompressManager> compress(CompressionTypeEnum compressionMode, File inputDirectory, List<File> fileList,
            File zipFile, File rootDirectory, Boolean flatArchive, Boolean runInThread, Charset aCharset)
            throws CompressionException {

        Vector<CompressManager> compressManagers = new Vector<>();

        // initialise concrete compression
        initCompression(compressionMode);

        // specify input file list
        List<File> validateFileList = validateFilesForCompress(fileList, inputDirectory);

        // Sets the root directory
        if (inputDirectory != null) {
            strategy.setRootDirectory(inputDirectory);
        } else {
            if (rootDirectory != null) {
                strategy.setRootDirectory(rootDirectory);
            } else {
                throw new CompressionException(String.format("The root directory must be set and cannot be null"));
            }
        }
        strategy.setFlatArchive(flatArchive.booleanValue());

        // Apply the encoding format
        strategy.setCharSet(aCharset);

        // Set synchrone or asynchrone compression mode
        strategy.setRunInThread(runInThread);

        /*
         * Check the size of the files to compress
         */
        List<File> listFile2Compress = new ArrayList<>();
        List<File> listFile2Big = new ArrayList<>();

        /*
         * Apply the max size for each file
         */
        long sizeTotal = 0;
        for (File tmpFile : validateFileList) {
            if (!isTooLargeFile(tmpFile)) {
                listFile2Compress.add(tmpFile);
                sizeTotal += tmpFile.length() / BYTES_IN_KILOBYTE;
            } else {
                listFile2Big.add(tmpFile);
                if (LOGGER.isInfoEnabled()) {
                    Long size = new Long(tmpFile.length() / BYTES_IN_KILOBYTE);
                    LOGGER.info(String.format(
                                              "The size of the file '%s' is %d ko, it exceeds the maximum size for the compression",
                                              tmpFile.getAbsoluteFile().getName(), size));
                }
            }
        }

        /*
         * If the total size does not exceed the max proceed the compress into one archive file
         */
        if (sizeTotal < maxArchiveSize) {
            if (listFile2Compress.size() > 0) {
                compressManagers.add(this.compress(listFile2Compress, zipFile));
            } else {
                if (LOGGER.isInfoEnabled()) {
                    Long sizeData = new Long(sizeTotal);
                    Long sizeMax = new Long(maxArchiveSize);
                    LOGGER.info(String.format(
                                              "The total file size to compress %d does not exceed the archive max size %d, then compress in one file",
                                              sizeData, sizeMax));
                }
            }
        } else {
            if (LOGGER.isInfoEnabled()) {
                Long sizeData = new Long(sizeTotal);
                Long sizeMax = new Long(maxArchiveSize);
                LOGGER.info(String.format(
                                          "The size of data is %d ko, it exceeds the maximum size %d ko for the compression, the compression is splitted in a multiple file.",
                                          sizeData, sizeMax));
            }
            /*
             * The total size exceed the max, split it in several files
             */
            if (runInThread) {
                strategy.setRunInThread(false);
                LOGGER.warn(String
                        .format("The size of data exceeds the maximum size, synchrone compression mode is used."));
            }

            List<File> listFileOneArchive = new ArrayList<>();
            int indiceArchive = 1;
            boolean compress = false;
            long tailleCourante = 0;

            for (File currentFile : listFile2Compress) {
                long currentFileSize = currentFile.length() / BYTES_IN_KILOBYTE;
                // check if the max size is attempt
                if (tailleCourante + currentFileSize < maxArchiveSize) {
                    listFileOneArchive.add(currentFile);
                    tailleCourante += currentFileSize;
                } else {
                    compress = true;
                }
                if (compress) {
                    Integer rang = new Integer(indiceArchive++);
                    File archiveName = new File(zipFile.getAbsoluteFile() + "_" + rang.toString());
                    compressManagers.add(this.compress(listFileOneArchive, archiveName));
                    listFileOneArchive.clear();

                    // add the current file into the next archive file
                    tailleCourante = currentFileSize;
                    listFileOneArchive.add(currentFile);
                    compress = false;
                }
            }

            if (listFileOneArchive.size() > 0) {
                Integer rang = new Integer(indiceArchive++);
                File archiveName = new File(zipFile.getAbsoluteFile() + "_" + rang.toString());
                compressManagers.add(this.compress(listFileOneArchive, archiveName));
                listFileOneArchive.clear();
            }
        }

        /*
         * Add to the result list the too big file
         */
        if (listFile2Big.size() > 0) {
            for (File tmpFile : listFile2Big) {
                CompressManager manager = new CompressManager();
                manager.setPercentage(100);
                manager.setCompressedFile(tmpFile);
                compressManagers.add(manager);
            }
        }

        return compressManagers;
    }

    /**
     * Cette methode permet de decompresser un fichier ou ensemble de fichier d'un repertoire vers un autre repertoire
     * 
     * @param compressionMode
     *            le type de compression (ZIP, GZIP, TAR, ...)
     * @param inputFile
     *            le fichier a décompresser
     * @param outputDirectory
     *            le repertoire cible
     * @throws CompressionException
     *             si l'un des paramètres n'est pas correct
     */
    public void decompress(CompressionTypeEnum compressionMode, File inputFile, File outputDirectory)
            throws CompressionException {
        decompress(compressionMode, inputFile, outputDirectory, null);
    }

    /**
     * Cette methode permet de decompresser un fichier ou ensemble de fichier d'un repertoire vers un autre repertoire
     * 
     * Cette méthode permet de determiner le type d'encodage des caractères lors de la decompression.
     * 
     * @param compressionMode
     *            le type de compression (ZIP, GZIP, TAR, ...)
     * @param inputFile
     *            le fichier a décompresser
     * @param outputDirectory
     *            le repertoire cible
     * 
     * @param aCharset
     *            Type d'encodage des caracteres utilisé lors de la decompression.
     * 
     * @throws CompressionException
     *             si l'un des paramètres n'est pas correct
     */
    public void decompress(CompressionTypeEnum compressionMode, File inputFile, File outputDirectory, Charset aCharset)
            throws CompressionException {

        if (!inputFile.isFile()) {
            throw new FileAlreadyExistException(
                    String.format("'%s' must be a file (not a directory)", inputFile.getName()));
        }

        strategy.setCompressedFile(inputFile);

        strategy.setCharSet(aCharset);

        // Checking directory existance
        validateFile(outputDirectory);
        if (!outputDirectory.isDirectory()) {
            throw new FileAlreadyExistException(
                    String.format("'%s' must be a directory (not a file)", outputDirectory.getName()));
        }

        // initialise concrete compression
        initCompression(compressionMode);

        // Checking file existance
        validateFile(inputFile);

        strategy.setOutputDir(outputDirectory);

        strategy.doUncompress();

    }

    /**
     * Permet de factoriser l'initialisation de la strategie concrete de compression
     * 
     * @param compressionMode
     *            le type de compression (ZIP, GZIP, TAR, ...)
     * @throws CompressionException
     *             si l'un des paramètres n'est pas correct
     */
    private void initCompression(CompressionTypeEnum compressionMode) throws CompressionException {
        if (compressionMode.equals(CompressionTypeEnum.ZIP)) {
            strategy.setCompression(new ZipCompression());
        }
        //        else if (pMode.equals(CompressionTypeEnum.GZIP)) {
        //            strategy.setCompression(new GZipCompression());
        //        } else if (pMode.equals(CompressionTypeEnum.TAR)) {
        //            strategy.setCompression(new TarCompression());
        //        } else if (pMode.equals(CompressionTypeEnum.Z)) {
        //            strategy.setCompression(new ZCompression());
        //        }
        else {
            throw new CompressionException(String.format("The compression mode %s is not defined", compressionMode.toString()));
        }
    }

    /**
     * Permet de verifier la validitée de la liste de fichiers ou du repertoire a compresser Crée une liste de fichiers
     * valides a partir d'une liste ou d'un repertoire
     * 
     * @param fileList
     *            une liste contenant des instances de File
     * @param inputDirectory
     *            le repertoire dans lesquels sont les fichiers
     * @return la liste de fichiers a compresser
     * @throws CompressionException
     *             si l'un des paramètres n'est pas correct
     */
    private List<File> validateFilesForCompress(List<File> fileList, File inputDirectory)
            throws CompressionException {

        List<File> retour = new ArrayList<>();

        if (fileList == null || fileList.isEmpty()) {
            retour = validateDirectoryForCompress(inputDirectory);
        } else {
            for (File tmpFile : fileList) {

                validateFile(tmpFile);

                if (!tmpFile.isFile()) {
                    throw new FileAlreadyExistException(
                            String.format("'%s' must be a file (not a directory)", tmpFile.getName()));
                }
                retour.add(tmpFile);
            }
        }
        return retour;
    }

    /**
     * Permet de verifier la validitée deu repertoire a compresser Crée une liste de fichiers valides a partir d'un
     * repertoire
     * 
     * @param pFileList
     *            une liste contenant des instances de File
     * @param inputDirectory
     *            le repertoire dans lesquels sont les fichiers
     * @return la liste de fichiers a compresser
     * @throws CompressionException
     *             si l'un des paramètres n'est pas correct
     */
    private List<File> validateDirectoryForCompress(File inputDirectory) throws CompressionException {

        List<File> retour = new ArrayList<>();

        if (inputDirectory == null || "".equals(inputDirectory.getName())) {
            throw new CompressionException(String.format("No file or directory are specified"));
        }

        // If pFileList is null or empty, then all files in directory are compressed
        validateFile(inputDirectory);

        if (!inputDirectory.isDirectory()) {
            throw new FileAlreadyExistException(
                    String.format("'%s' must be a directory (not a file)", inputDirectory.getName()));
        }

        File[] tabFile = inputDirectory.listFiles();
        // If the current directory is empty, then add the directory to the list of file to compress
        if (tabFile.length == 0) {
            retour.add(inputDirectory);
        } else {

            for (File file : tabFile) {
                if (file.isFile()) {
                    retour.add(file);
                } else {
                    // this is a directory
                    retour.addAll(validateFilesForCompress(null, file));
                }
            }
        }

        return retour;
    }

    /**
     * Vérifie qu'un fichier existe et est lisible
     * 
     * @param file
     *            le fichier a vérivier
     * @throws CompressionException
     *             si l'un des paramètres n'est pas correct
     */
    private void validateFile(File file) throws CompressionException {
        if (file == null) {
            throw new CompressionException(String.format("File parameter is null"));
        }

        if (!file.exists()) {
            throw new CompressionException(String.format("The file or directory '%s' does not exist", file.getName()));
        }

        if (!file.canRead()) {
            throw new CompressionException(
                    String.format("The file or directory '%s' is not readable", file.getName()));
        }
    }

    /**
     * Verifie qu'un fichier peut etre compresse. Il doit etre de taille inférieure a MAX_SIZE_ARCHIVE
     * 
     * @param pFile
     *            le fichier a vérifier
     * 
     * @return vrai si le fichier est de taille inferieure a MAX_SIZE_ARCHIVE
     */
    public boolean isTooLargeFile(File pfile) {
        return pfile.length() / BYTES_IN_KILOBYTE > maxArchiveSize;
    }

    public void setMaxArchiveSize(long archiveSize) {
        this.maxArchiveSize = archiveSize;
    }

}
