/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.file.compression;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Class AbstractRunnableCompression
 *
 * Cette classe permet de rendre asynchrone la compression.
 */
public abstract class AbstractRunnableCompression implements ICompression {

    /**
     * Méthode permettant de lancer la compression de manière synchrone ou asychrone en précisant l'encodage de la
     * compression
     * @return CompressManager
     * @parameter fileList : Liste des fichiers a compresser
     * @parameter compressedFile : Nom du fichier archive sans extension
     * @parameter rootDirectory : Répertoire root des fichiers a archiver
     * @parameter flatArchive : Archivage a plat ou non
     * @parameter charset : Type d'encodage
     */
    @Override
    public CompressManager compress(List<File> fileList, File compressedFile, File rootDirectory, boolean flatArchive,
            boolean runInThread, Charset charset) throws CompressionException {
        if (runInThread) {
            return runThreadCompress(fileList, compressedFile, rootDirectory, flatArchive, charset);
        } else {
            return runCompress(fileList, compressedFile, rootDirectory, flatArchive, charset, new CompressManager());
        }
    }

    /**
     * Méthode permettant de lancer la compression de manière synchrone ou asychrone.
     * @return CompressManager
     * @parameter fileList : Liste des fichiers a compresser
     * @parameter compressedFile : Nom du fichier archive sans extension
     * @parameter rootDirectory : Répertoire root des fichiers a archiver
     * @parameter flatArchive : Archivage a plat ou non
     * @parameter charset : Type d'encodage
     */
    @Override
    public CompressManager compress(List<File> fileList, File compressedFile, File rootDirectory, boolean flatArchive,
            boolean runInThread) throws CompressionException {
        if (runInThread) {
            return runThreadCompress(fileList, compressedFile, rootDirectory, flatArchive, null);
        } else {
            return runCompress(fileList, compressedFile, rootDirectory, flatArchive, null, new CompressManager());
        }

    }

    /**
     * Methode permettant de lancer la compression de manière asynchrone dans un thread.
     * @return CompressManager
     * @parameter fileList : Liste des fichiers a compresser
     * @parameter compressedFile : Nom du fichier archive sans extension
     * @parameter rootDirectory : Répertoire root des fichiers a archiver
     * @parameter flatArchive : Archivage a plat ou non
     * @parameter charset : Type d'encodage
     */
    private CompressManager runThreadCompress(List<File> fileList, File compressedFile, File rootDirectory,
            boolean flatArchive, Charset charset) throws CompressionException {

        CompressionRunImpl impl = new CompressionRunImpl(this, fileList, compressedFile, rootDirectory, flatArchive,
                charset);
        Thread thread = new Thread(impl);
        thread.start();

        return impl.getCompressManager();
    }

    /**
     * Méthode permettant de lancer la compression de manière synchrone
     * @parameter fileList : Liste des fichiers a compresser
     * @parameter compressedFile : Nom du fichier archive sans extension
     * @parameter rootDirectory : Répertoire root des fichiers a archiver
     * @parameter flatArchive : Archivage a plat ou non
     * @parameter charset : Type d'encodage
     * @parameter compressManager : Gestionnaire de compression
     */
    public void compress(List<File> fileList, File compressedFile, File rootDirectory, boolean flatArchive,
            Charset charset, CompressManager compressManager) throws CompressionException {
        runCompress(fileList, compressedFile, rootDirectory, flatArchive, charset, compressManager);
    }

    /**
     * Méthode permettant de réaliser la compression
     * @return CompressManager
     */
    protected abstract CompressManager runCompress(List<File> fileList, File compressedFile, File rootDirectory,
            boolean flatArchive, Charset charset, CompressManager compressManager) throws CompressionException;
}
