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
package fr.cnes.regards.framework.utils.file.compression;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class CompressionRunImpl
 *
 * Classe représentant le traintement asynchrone d'une compression dans un thread.
 */
public class CompressionRunImpl implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompressionRunImpl.class);

    private final AbstractRunnableCompression compression;

    private final List<File> fileList;

    private final File compressedFile;

    private final File rootDirectory;

    private final boolean flatArchive;

    private final Charset charset;

    private final CompressManager compressManager = new CompressManager();

    public CompressManager getCompressManager() {
        return compressManager;
    }

    public CompressionRunImpl(AbstractRunnableCompression pCompression, List<File> pFileList, File pCompressedFile,
            File pRootDirectory, boolean pFlatArchive, Charset pCharset) {
        compression = pCompression;
        fileList = pFileList;
        compressedFile = pCompressedFile;
        rootDirectory = pRootDirectory;
        flatArchive = pFlatArchive;
        charset = pCharset;
    }

    @Override
    public void run() {

        try {
            LOGGER.info("Running thread compress");
            compressManager.setThread(Thread.currentThread());
            compression.runCompress(fileList, compressedFile, rootDirectory, flatArchive, charset, compressManager);
        } catch (CompressionException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

}
