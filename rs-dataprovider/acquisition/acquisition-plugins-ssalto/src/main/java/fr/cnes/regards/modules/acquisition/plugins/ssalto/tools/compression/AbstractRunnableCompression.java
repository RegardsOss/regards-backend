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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.compression;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.compression.exception.CompressionException;

/**
 * Cette classe permet de rendre asynchrone la compression
 * 
 * @author Christophe Mertz
 *
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
