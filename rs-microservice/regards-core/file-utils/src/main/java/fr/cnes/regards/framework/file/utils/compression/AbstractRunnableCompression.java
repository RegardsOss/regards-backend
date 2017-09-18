/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.file.utils.compression;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 *
 * Class AbstractRunnableCompression
 *
 * Cette classe permet de rendre asynchrone la compression.
 */
public abstract class AbstractRunnableCompression implements ICompression {

    /**
     * Méthode permettant de lancer la compression de manière synchrone ou asychrone en précisant l'encodage de la
     * compression
     *
     * @parameter pFileList : Liste des fichiers a compresser
     * @parameter pCompressedFile : Nom du fichier archive sans extension
     * @parameter pRootDirectory : Répertoire root des fichiers a archiver
     * @parameter pFlatArchive : Archivage a plat ou non
     * @parameter pCharset : Type d'encodage
     *
     * @return CompressManager
     * @throws CompressionException
     */
    @Override
    public CompressManager compress(List<File> pFileList, File pCompressedFile, File pRootDirectory,
            boolean pFlatArchive, boolean pRunInThread, Charset pCharset) throws CompressionException {
        if (pRunInThread) {
            return runThreadCompress(pFileList, pCompressedFile, pRootDirectory, pFlatArchive, pCharset);
        } else {
            return runCompress(pFileList, pCompressedFile, pRootDirectory, pFlatArchive, pCharset,
                               new CompressManager());
        }
    }

    /**
     * Méthode permettant de lancer la compression de manière synchrone ou asychrone.
     *
     * @parameter pFileList : Liste des fichiers a compresser
     * @parameter pCompressedFile : Nom du fichier archive sans extension
     * @parameter pRootDirectory : Répertoire root des fichiers a archiver
     * @parameter pFlatArchive : Archivage a plat ou non
     * @parameter pCharset : Type d'encodage
     *
     * @return CompressManager
     * @throws CompressionException
     */
    @Override
    public CompressManager compress(List<File> pFileList, File pCompressedFile, File pRootDirectory,
            boolean pFlatArchive, boolean pRunInThread) throws CompressionException {
        if (pRunInThread) {
            return runThreadCompress(pFileList, pCompressedFile, pRootDirectory, pFlatArchive, null);
        } else {
            return runCompress(pFileList, pCompressedFile, pRootDirectory, pFlatArchive, null, new CompressManager());
        }

    }

    /**
     * Methode permettant de lancer la compression de manière asynchrone dans un thread.
     *
     * @parameter pFileList : Liste des fichiers a compresser
     * @parameter pCompressedFile : Nom du fichier archive sans extension
     * @parameter pRootDirectory : Répertoire root des fichiers a archiver
     * @parameter pFlatArchive : Archivage a plat ou non
     * @parameter pCharset : Type d'encodage
     *
     * @return CompressManager
     * @throws CompressionException
     */
    private CompressManager runThreadCompress(List<File> pFileList, File pCompressedFile, File pRootDirectory,
            boolean pFlatArchive, Charset pCharset) throws CompressionException {

        CompressionRunImpl impl = new CompressionRunImpl(this, pFileList, pCompressedFile, pRootDirectory, pFlatArchive,
                pCharset);
        Thread thread = new Thread(impl);
        thread.start();

        return impl.getCompressManager();
    }

    /**
     * Méthode permettant de lancer la compression de manière synchrone
     *
     * @parameter pFileList : Liste des fichiers a compresser
     * @parameter pCompressedFile : Nom du fichier archive sans extension
     * @parameter pRootDirectory : Répertoire root des fichiers a archiver
     * @parameter pFlatArchive : Archivage a plat ou non
     * @parameter pCharset : Type d'encodage
     * @parameter pCompressManager : Gestionnaire de compression
     *
     * @return void
     * @throws CompressionException
     *
     */
    public void compress(List<File> pFileList, File pCompressedFile, File pRootDirectory, boolean pFlatArchive,
            Charset pCharset, CompressManager pCompressManager) throws CompressionException {
        runCompress(pFileList, pCompressedFile, pRootDirectory, pFlatArchive, pCharset, pCompressManager);
    }

    /**
     *
     * Méthode permettant de réaliser la compression
     *
     * @param pFileList
     * @param pCompressedFile
     * @param pRootDirectory
     * @param pFlatArchive
     * @param pCharset
     * @param pCompressManager
     * @return CompressManager
     * @throws CompressionException
     */
    protected abstract CompressManager runCompress(List<File> pFileList, File pCompressedFile, File pRootDirectory,
            boolean pFlatArchive, Charset pCharset, CompressManager pCompressManager) throws CompressionException;
}
