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
 * 
 * Class CompressionRunImpl
 * 
 * Classe représentant le traintement asynchrone d'une compression dans un thread.
 * 
 * @author CS
 * @since 5.4
 */
public class CompressionRunImpl implements Runnable {

    private AbstractRunnableCompression compression_ = null;

    private final List<File> fileList_;

    private final File compressedFile_;

    private final File rootDirectory_;

    private final boolean flatArchive_;

    private final Charset charset_;

    private final CompressManager compressManager_ = new CompressManager();

    public CompressManager getCompressManager() {
        return compressManager_;
    }

    public CompressionRunImpl(AbstractRunnableCompression pCompression, List<File> pFileList, File pCompressedFile,
            File pRootDirectory, boolean pFlatArchive, Charset pCharset) {
        compression_ = pCompression;
        fileList_ = pFileList;
        compressedFile_ = pCompressedFile;
        rootDirectory_ = pRootDirectory;
        flatArchive_ = pFlatArchive;
        charset_ = pCharset;
    }

    @Override
    public void run() {

        try {
            System.out.println("Runnin compress thread");
            compressManager_.setThread(Thread.currentThread());
            compression_.runCompress(fileList_, compressedFile_, rootDirectory_, flatArchive_, charset_,
                                     compressManager_);
        }
        catch (CompressionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
