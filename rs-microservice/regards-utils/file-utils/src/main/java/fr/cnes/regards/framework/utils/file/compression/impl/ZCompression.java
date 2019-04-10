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
package fr.cnes.regards.framework.utils.file.compression.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.utils.file.compression.AbstractRunnableCompression;
import fr.cnes.regards.framework.utils.file.compression.CompressManager;
import fr.cnes.regards.framework.utils.file.compression.CompressionException;
import fr.cnes.regards.framework.utils.file.compression.CompressionTypeEnum;
import fr.cnes.regards.framework.utils.file.compression.FileAlreadyExistException;

/**
 * Manage Z (de)compression
 *
 * @author Marc SORDI
 *
 */
public class ZCompression extends AbstractRunnableCompression {

    private static Logger LOGGER = LoggerFactory.getLogger(GZipCompression.class);

    /**
     * Z extension
     */
    private static final String Z_EXTENSION = ".z";

    /**
     * Buffer size
     */
    private static final int BUFFER_SIZE = 1024;

    @Override
    public void uncompress(File compressedFile, File outputDir) throws CompressionException {
        uncompress(compressedFile, outputDir, null);
    }

    @Override
    public void uncompress(File compressedFile, File outputDir, Charset charset) throws CompressionException {

        // Compressed file must have .z extension
        if (!compressedFile.getName().toLowerCase().endsWith(Z_EXTENSION)) {
            String msg = String.format("Extension must be %s ", Z_EXTENSION);
            LOGGER.error(msg);
            throw new CompressionException(msg);
        }

        // Init result file
        String resultFilename = compressedFile.getName()
                .substring(0, compressedFile.getName().length() - Z_EXTENSION.length());
        Path resultPath = outputDir.toPath().resolve(resultFilename);

        if (Files.exists(resultPath)) {
            String msg = String.format("File %s already exist", resultPath);
            LOGGER.error(msg);
            throw new FileAlreadyExistException(msg);
        }

        // Uncompress
        try (ZCompressorInputStream zIn = new ZCompressorInputStream(
                new BufferedInputStream(Files.newInputStream(compressedFile.toPath())));
                OutputStream out = Files.newOutputStream(resultPath)) {
            final byte[] buffer = new byte[BUFFER_SIZE];
            int n = 0;
            while (-1 != (n = zIn.read(buffer))) {
                out.write(buffer, 0, n);
            }
        } catch (IOException e) {
            String msg = String
                    .format(String.format("IO error during %s uncompression of %s", CompressionTypeEnum.Z, resultPath));
            LOGGER.error(msg, e);
            throw new CompressionException(msg);
        }
    }

    @Override
    protected CompressManager runCompress(List<File> fileList, File compressedFile, File rootDirectory,
            boolean flatArchive, Charset charset, CompressManager compressManager) throws CompressionException {
        throw new CompressionException("Unsupported operation");
    }

}
