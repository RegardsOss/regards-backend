/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.utils.file.compression;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.utils.file.compression.gzip.GZipCompression;
import fr.cnes.regards.framework.utils.file.compression.tar.TarCompression;
import fr.cnes.regards.framework.utils.file.compression.zip.ZipCompression;

/**
 * Cette classe est la facade du paquetage de compression. La Facade cree une instance de Compression, et la passe au
 * contexte de compression qui s'occupe de l'execution. Les informations necessaires a la compression sont gerees au
 * niveau du contexte "CompressionContext".
 */
public class CompressionFacade {

    /**
     * Nombre d'octets dans un Ko.
     */
    private static final int BYTES_IN_KILOBYTE = 1024;

    /**
     * Nombre d'octets dans un Ko (en double).
     */
    public static final double BYTES_IN_KILOBYTE_DOUBLE = 1024.0;

    /**
     * Le contexte de compression qui pilote la compression proprement dite
     */
    private final CompressionContext strategy;

    /**
     * Attribut permettant la journalisation.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CompressionFacade.class);

    /**
     * Taille max des fichiers compresses en ko : 2Go valeur par défaut
     */
    private long maxArchiveSize = 2000000;

    /**
     * Constructeur par defaut
     */
    public CompressionFacade() {
        strategy = new CompressionContext();
    }



    /**
     * Compression d'une liste de fichiers
     * @param fileList une liste contenant les fichiers a compresser (classe File)
     * @param pZipFile definit le chemin et le nom du fichier compresse SANS l'extension
     * @return le fichier compresse
     */
    private CompressManager compress(List<File> fileList, File pZipFile) throws CompressionException {

        // specify input file list
        strategy.setInputSource(fileList);

        // specify compressed file
        strategy.setCompressedFile(pZipFile);

        CompressManager manager = strategy.doCompress();

        if (!strategy.isRunInThread() && (manager.getCompressedFile() != null) && (manager.getCompressedFile().length()
                == 0)) {
            throw new CompressionException("Error compressing files");
        }

        return manager;
    }

    /**
     * Cette methode permet de compresser un fichier ou ensemble de fichier d'un repertoire vers un autre repertoire La
     * liste des fichiers a compresser est precisee, mais si le parametre pFileList est nul tout le repertoire en entree
     * est compresse.
     * @param pMode le type de compression (ZIP, GZIP, TAR, ...)
     * @param pInputDirectory repertoire source
     * @param pFileList une liste contenant les fichiers a compresser (classe File)
     * @param pZipFile definit le chemin et le nom du fichier compresse SANS l'extension
     * @param pRootDirectory le répertoire racine dans le cas d'une liste de fichiers.
     * @param pFlatArchive contenu de l'archive à plat ou non
     * @return la liste des fichiers compresses
     * @throws CompressionException si l'un des paramètres n'est pas correct
     */
    public Vector<CompressManager> compress(CompressionTypeEnum pMode, File pInputDirectory, List<File> pFileList,
            File pZipFile, File pRootDirectory, Boolean pFlatArchive, Boolean pRunInThread)
            throws CompressionException {

        return compress(pMode, pInputDirectory, pFileList, pZipFile, pRootDirectory, pFlatArchive, pRunInThread, null);

    }

    /**
     * Cette methode permet de compresser un fichier ou ensemble de fichier d'un repertoire vers un autre repertoire La
     * liste des fichiers a compresser est precisee, mais si le parametre pFileList est nul tout le repertoire en entree
     * est compresse.
     *
     * Cette methode permet de définir l'encodage utilisé pour la compression.
     *
     * Attention : l'encodage des caractères n'est implémenté que pour le format ZIP.
     * @param pMode le type de compression (ZIP, GZIP, TAR, ...)
     * @param pInputDirectory repertoire source
     * @param pFileList une liste contenant les fichiers a compresser (classe File)
     * @param pZipFile definit le chemin et le nom du fichier compresse SANS l'extension
     * @param pRootDirectory le répertoire racine dans le cas d'une liste de fichiers.
     * @param pFlatArchive contenu de l'archive à plat ou non
     * @param pCharset Encodage des caractères utilisé lors de la compression.
     * @return la liste des fichiers compresses
     * @throws CompressionException si l'un des paramètres n'est pas correct
     */
    public Vector<CompressManager> compress(CompressionTypeEnum pMode, File pInputDirectory, List<File> pFileList,
            File pZipFile, File pRootDirectory, Boolean pFlatArchive, Boolean pRunInThread, Charset pCharset)
            throws CompressionException {

        Vector<CompressManager> compressManagers = new Vector<>();

        // initialise concrete compression
        initCompression(pMode);

        // specify input file list
        List<File> fileList = validateFilesForCompress(pFileList, pInputDirectory);

        // Sets the root directory
        if (pInputDirectory != null) {
            strategy.setRootDirectory(pInputDirectory);
        } else {
            if (pRootDirectory != null) {
                strategy.setRootDirectory(pRootDirectory);
            } else {
                throw new CompressionException("The root directory must be set and cannot be null.");
            }
        }
        strategy.setFlatArchive(pFlatArchive.booleanValue());

        // Apply the encoding format
        strategy.setCharSet(pCharset);

        // Set synchrone or asynchrone compression mode
        strategy.setRunInThread(pRunInThread);

        /*
         * Check the size of the files to compress
         */
        List<File> listFile2Compress = new ArrayList<>();
        List<File> listFile2Big = new ArrayList<>();

        /*
         * Apply the max size for each file
         */
        long sizeTotal = 0;
        for (File tmpFile : fileList) {
            if (!isTooLargeFile(tmpFile)) {
                listFile2Compress.add(tmpFile);
                sizeTotal += tmpFile.length() / BYTES_IN_KILOBYTE;
            } else {
                listFile2Big.add(tmpFile);
                if (LOGGER.isInfoEnabled()) {
                    long size = tmpFile.length() / BYTES_IN_KILOBYTE;
                    final String msg = String
                            .format("The size of the file %s is %d ko, it exceeds the maximum size for the compression.",
                                    tmpFile.getAbsoluteFile(), size);
                    LOGGER.info(msg);
                }
            }
        }

        /*
         * If the total size does not exceed the max proceed the compress into one archive file
         */
        if (sizeTotal < maxArchiveSize) {
            if (!listFile2Compress.isEmpty()) {
                compressManagers.add(this.compress(listFile2Compress, pZipFile));
            } else {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("No file to compress");
                }
            }
        } else {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format(
                        "The size of data is %d ko, it exceeds the maximum size %d ko for the compression, the compression is splitted in a multiple file.",
                        sizeTotal, maxArchiveSize));
            }
            /*
             * The total size exceed the max, split it in several files
             */
            if (pRunInThread) {
                strategy.setRunInThread(false);
                LOGGER.warn("The size of data exceeds the maximum size, synchrone compression mode is used.");
            }

            List<File> listFileOneArchive = new ArrayList<>();
            int indiceArchive = 1;
            boolean compress = false;
            long tailleCourante = 0;

            for (File currentFile : listFile2Compress) {
                long currentFileSize = currentFile.length() / BYTES_IN_KILOBYTE;
                // check if the max size is attempt
                if ((tailleCourante + currentFileSize) < maxArchiveSize) {
                    listFileOneArchive.add(currentFile);
                    tailleCourante += currentFileSize;
                } else {
                    compress = true;
                }
                if (compress) {
                    indiceArchive++;
                    File archiveName = new File(pZipFile.getAbsoluteFile() + "_" + Integer.toString(indiceArchive));
                    compressManagers.add(this.compress(listFileOneArchive, archiveName));
                    listFileOneArchive.clear();

                    // add the current file into the next archive file
                    tailleCourante = currentFileSize;
                    listFileOneArchive.add(currentFile);
                    compress = false;
                }
            }

            if (!listFileOneArchive.isEmpty()) {
                indiceArchive++;
                File archiveName = new File(pZipFile.getAbsoluteFile() + "_" + Integer.toString(indiceArchive));
                compressManagers.add(this.compress(listFileOneArchive, archiveName));
                listFileOneArchive.clear();
            }
        }

        /*
         * Add to the result list the too big file
         */
        if (!listFile2Big.isEmpty()) {
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
     * @param pMode le type de compression (ZIP, GZIP, TAR, ...)
     * @param pInputFile le fichier a décompresser
     * @param pOutputDirectory le repertoire cible
     * @throws CompressionException si l'un des paramètres n'est pas correct
     */
    public void decompress(CompressionTypeEnum pMode, File pInputFile, File pOutputDirectory)
            throws CompressionException {
        decompress(pMode, pInputFile, pOutputDirectory, null);
    }

    /**
     * Cette methode permet de decompresser un fichier ou ensemble de fichier d'un repertoire vers un autre repertoire
     *
     * Cette méthode permet de determiner le type d'encodage des caractères lors de la decompression.
     * @param pMode le type de compression (ZIP, GZIP, TAR, ...)
     * @param pInputFile le fichier a décompresser
     * @param pOutputDirectory le repertoire cible
     * @param pCharset Type d'encodage des caracteres utilisé lors de la decompression.
     * @throws CompressionException si l'un des paramètres n'est pas correct
     */
    public void decompress(CompressionTypeEnum pMode, File pInputFile, File pOutputDirectory, Charset pCharset)
            throws CompressionException {

        if (!pInputFile.isFile()) {
            throw new FileAlreadyExistException(
                    String.format("%s must be a file (not a directory)", pInputFile.getAbsolutePath()));
        }

        strategy.setCompressedFile(pInputFile);

        strategy.setCharSet(pCharset);

        // Checking directory existance
        validateFile(pOutputDirectory);
        if (!pOutputDirectory.isDirectory()) {
            throw new FileAlreadyExistException(
                    String.format("%s must be a directory (not a file)", pOutputDirectory.getAbsolutePath()));
        }

        // initialise concrete compression
        initCompression(pMode);

        // Checking file existance
        validateFile(pInputFile);

        strategy.setOutputDir(pOutputDirectory);

        strategy.doUncompress();

    }

    /**
     * Permet de factoriser l'initialisation de la strategie concrete de compression
     * @param pMode le type de compression (ZIP, GZIP, TAR, ...)
     * @throws CompressionException si l'un des paramètres n'est pas correct
     */
    private void initCompression(CompressionTypeEnum pMode) throws CompressionException {
        if (pMode.equals(CompressionTypeEnum.ZIP)) {
            strategy.setCompression(new ZipCompression());
        } else if (pMode.equals(CompressionTypeEnum.GZIP)) {
            strategy.setCompression(new GZipCompression());
        } else if (pMode.equals(CompressionTypeEnum.TAR)) {
            strategy.setCompression(new TarCompression());
        } else {
            throw new CompressionException(String.format("The compression mode %s is not defined", pMode));
        }
    }

    /**
     * Permet de verifier la validitée de la liste de fichiers ou du repertoire a compresser Crée une liste de fichiers
     * valides a partir d'une liste ou d'un repertoire
     * @param pFileList une liste contenant des instances de File
     * @param pInputDirectory le repertoire dans lesquels sont les fichiers
     * @return la liste de fichiers a compresser
     * @throws CompressionException si l'un des paramètres n'est pas correct
     */
    private List<File> validateFilesForCompress(List<File> pFileList, File pInputDirectory)
            throws CompressionException {

        List<File> retour = new ArrayList<>();

        if ((pFileList == null) || pFileList.isEmpty()) {
            retour = validateDirectoryForCompress(pInputDirectory);
        } else {
            for (File tmpFile : pFileList) {

                validateFile(tmpFile);

                if (!tmpFile.isFile()) {
                    throw new FileAlreadyExistException(
                            String.format("%s must be a file (not a directory)", tmpFile.getAbsolutePath()));
                }
                retour.add(tmpFile);
            }
        }
        return retour;
    }

    /**
     * Permet de verifier la validitée deu repertoire a compresser Crée une liste de fichiers valides a partir d'un
     * repertoire
     * @param pInputDirectory le repertoire dans lesquels sont les fichiers
     * @return la liste de fichiers a compresser
     * @throws CompressionException si l'un des paramètres n'est pas correct
     */
    private List<File> validateDirectoryForCompress(File pInputDirectory) throws CompressionException {

        List<File> retour = new ArrayList<>();

        if ((pInputDirectory == null) || pInputDirectory.getName().equals("")) {
            throw new CompressionException("No file or directory are specified");
        }

        // If pFileList is null or empty, then all files in directory are compressed
        validateFile(pInputDirectory);

        if (!pInputDirectory.isDirectory()) {
            throw new FileAlreadyExistException(
                    String.format("%s must be a directory (not a file)", pInputDirectory.getAbsolutePath()));
        }

        File[] tabFile = pInputDirectory.listFiles();
        // If the curent directory is empty, then add the directory to the list of file to compress
        if (tabFile.length == 0) {
            retour.add(pInputDirectory);
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
     * @param pFile le fichier a vérivier
     * @throws CompressionException si l'un des paramètres n'est pas correct
     */
    private void validateFile(File pFile) throws CompressionException {
        if (pFile == null) {
            throw new CompressionException("File parameter is null");
        }

        if (!pFile.exists()) {
            throw new CompressionException(
                    String.format("The file or directory %s does not exist", pFile.getAbsoluteFile()));
        }

        if (!pFile.canRead()) {
            throw new CompressionException(
                    String.format("The compression mode %s is not defined", pFile.getAbsoluteFile()));
        }
    }

    /**
     * Verifie qu'un fichier peut etre compresse. Il doit etre de taille inférieure a MAX_SIZE_ARCHIVE
     * @param pfile le fichier a vérifier
     * @return vrai si le fichier est de taille inferieure a MAX_SIZE_ARCHIVE
     */
    public boolean isTooLargeFile(File pfile) {
        return (pfile.length() / BYTES_IN_KILOBYTE) > maxArchiveSize;
    }

    public void setMaxArchiveSize(long pMaxArchiveSize_) {
        this.maxArchiveSize = pMaxArchiveSize_;
    }

}
