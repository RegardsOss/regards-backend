package fr.cnes.regards.framework.file.utils.compression;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 *
 * Interface des classes realisant une compression. Elle definit les parametres et methodes des classes devant
 * implementer une compression concrete.
 *
 * @author CS
 * @version $Revision: 1.8 $
 * @since 1.0
 */
public interface ICompression {

    /**
     * Permet de compression une liste de fichiers dans un seul.
     *
     * @param pFileList
     *            la liste de <code>File</code> a compresser
     * @param pCompressedFile
     *            définit le nom et le chemin du fichier compressé sans extension
     * @param pRootDirectory
     *            le répertoire racine de tous les fichiers à compresser.
     * @param pFlatArchive
     *            indique si les fichiers sont ajoutes a plat dans le tar sans tenir compte de l'arborescence.
     *
     * @param pCharset
     *            indique le format d'encodage des caractères lors de la compression.
     * @return le fichier compressé avec l'extension
     * @throws CompressionException
     *             si l'un des paramètres est incorrect ou illisible
     * @since 5.3
     * @DM SIPNG-DM-0121-CN : Gestion du format d'encodage des caracteres lors de la compression et decompression des
     *     archives
     */
    public abstract CompressManager compress(List<File> pFileList, File pCompressedFile, File pRootDirectory,
            boolean pFlatArchive, boolean pRunInThread, Charset pCharset) throws CompressionException;

    /**
     * Permet de compression une liste de fichiers dans un seul. le format d'encodage des caractère utilisé est celui du
     * système.
     *
     * @param pFileList
     *            la liste de <code>File</code> a compresser
     * @param pCompressedFile
     *            définit le nom et le chemin du fichier compressé sans extension
     * @param pRootDirectory
     *            le répertoire racine de tous les fichiers à compresser.
     * @param pFlatArchive
     *            indique si les fichiers sont ajoutes a plat dans le tar sans tenir compte de l'arborescence.
     * @return le fichier compressé avec l'extension
     * @throws CompressionException
     *             si l'un des paramètres est incorrect ou illisible
     * @since 1.0
     * @FA SIPNG-FA-0450-CN : ajout d'un parametre
     */
    public abstract CompressManager compress(List<File> pFileList, File pCompressedFile, File pRootDirectory,
            boolean pFlatArchive, boolean pRunInThread) throws CompressionException;

    /**
     * Permet de decompresser un fichier compresse dans un repertoire cible
     *
     * @param pCompressedFile
     *            le fichier a decompresser
     * @param pOutputDir
     *            le repertoire destination
     * @throws CompressionException
     *             si l'un des paramètres est incorrect ou illisible
     * @since 5.3
     * @DM SIPNG-DM-0121-CN : Gestion du format d'encodage des caracteres lors de la compression et decompression des
     *     archives
     */
    public abstract void uncompress(File pCompressedFile, File pOutputDir, Charset pCharset)
            throws CompressionException;

    /**
     * Permet de decompresser un fichier compresse dans un repertoire cible
     *
     * @param pCompressedFile
     *            le fichier a decompresser
     * @param pOutputDir
     *            le repertoire destination
     * @throws CompressionException
     *             si l'un des paramètres est incorrect ou illisible
     * @since 1.0
     */
    public abstract void uncompress(File pCompressedFile, File pOutputDir) throws CompressionException;
}
