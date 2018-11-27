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
package fr.cnes.regards.framework.utils.file;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * Utils to handle files.
 * @author Sébastien Binda
 */
public final class CommonFileUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CommonFileUtils.class);

    private CommonFileUtils() {
    }

    /**
     * Return the first non existing file name into the pDirectory {@link Path} given and related to the given {@link String} pOrigineFileName.
     * If a file exists in the pDirectory with the pOrigineFileName as name, so this method return a file name as :<br/>
     * [pOrigineFileName without extension]_[i].[pOrigineFileName extension] where i is an integer.
     * @param pDirectory {@link Path} Directory to scan for existings files.
     * @param pOrigineFileName {@link String} Original file name wanted.
     * @return {@link String} First available file name.
     * @throws IOException Error reading the {@link Path} pDirectory
     */
    public static String getAvailableFileName(Path pDirectory, String pOrigineFileName) throws IOException {

        String availableFileName = pOrigineFileName;

        int cpt = 1;
        // Get all existing file names
        Set<String> fileNames = Sets.newHashSet();
        Files.walk(pDirectory, 1).forEach(f -> fileNames.add(f.getFileName().toString()));
        while (fileNames.contains(availableFileName)) {
            int index = availableFileName.indexOf('.');
            if (index > 0) {
                availableFileName = String.format("%s_%d.%s", availableFileName.substring(0, index), cpt,
                                                  availableFileName.substring(index + 1));
            }
            cpt++;
        }

        return availableFileName;

    }

    /**
     * Write from a given {@link FileInputStream} to the output given {@link File} a maximum of <maxSizeToWrite> bytes.
     * @param pInputStream {@link FileInputStream} reader
     * @param outputFile {@link File} to write to
     * @param maxSizeToWrite maximum number of bytes to write
     * @return TRUE if there is more bytes to read from pInputStream after writing the maximum number of bytes.
     * @throws IOException I/O exception.
     */
    public static void writeInFile(FileInputStream pInputStream, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int charRead;
        while ((charRead = pInputStream.read(buffer)) > 0) {
            os.write(buffer, 0, charRead);
        }
        os.flush();
    }

    /**
     * Gets image dimensions for given file
     * @param imgFile image file
     * @return dimensions of image
     * @throws IOException if the file is not a known image
     */
    public static Dimension getImageDimension(File imgFile) throws IOException {
        int pos = imgFile.getName().lastIndexOf(".");
        if (pos == -1) {
            throw new IOException("No extension for file: " + imgFile.getAbsolutePath());
        }
        String suffix = imgFile.getName().substring(pos + 1);
        Iterator<ImageReader> iter = ImageIO.getImageReadersBySuffix(suffix);
        while (iter.hasNext()) {
            ImageReader reader = iter.next();
            try (ImageInputStream stream = new FileImageInputStream(imgFile)) {
                reader.setInput(stream);
                int width = reader.getWidth(reader.getMinIndex());
                int height = reader.getHeight(reader.getMinIndex());
                return new Dimension(width, height);
            } catch (IOException e) {
                LOG.warn("Error reading: " + imgFile.getAbsolutePath(), e);
            } finally {
                reader.dispose();
            }
        }

        throw new IOException("Not a known image file: " + imgFile.getAbsolutePath());
    }

}
