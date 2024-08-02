/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Utils to handle files.
 *
 * @author Sébastien Binda
 */
public final class CommonFileUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CommonFileUtils.class);

    private CommonFileUtils() {
    }

    /**
     * Return the first non existing file name into the directory {@link Path} given and related to the given {@link String} originFileName.
     * If a file exists in the directory with the originFileName as name, so this method return a file name as :<br/>
     * [originFileName without extension]_[i].[originFileName extension] where i is an integer if filename has an extension.<br/>
     * [originFileName without extension]_[i] where i is an integer if filename has no extension.
     *
     * @param directory      {@link Path} Directory to scan for existing files.
     * @param originFileName {@link String} Original file name wanted.
     * @return {@link String} First available file name.
     * @throws IOException Error reading the {@link Path} directory
     */
    public static String getAvailableFileName(Path directory, String originFileName) throws IOException {

        String availableFileName = originFileName;

        int cpt = 1;
        // Get all existing file names
        Set<String> fileNames = Sets.newHashSet();
        try (Stream<Path> walk = Files.walk(directory, 1)) {
            walk.forEach(f -> fileNames.add(f.getFileName().toString()));
        }
        while (fileNames.contains(availableFileName)) {
            int index = availableFileName.lastIndexOf('.');
            if (index > 0) {
                // with lastIndexOf we are sure to get the extension
                availableFileName = String.format("%s_%d.%s",
                                                  availableFileName.substring(0, index),
                                                  cpt,
                                                  availableFileName.substring(index + 1));
            } else {
                // handle case when file has no extension to avoid infinite loop
                availableFileName = String.format("%s_%d", availableFileName, cpt);
            }
            cpt++;
        }

        return availableFileName;

    }

    /**
     * Write from a given {@link FileInputStream} to the output given {@link File} a maximum of <maxSizeToWrite> bytes.
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
     *
     * @param imgFile image file
     * @return dimensions of image
     * @throws IOException if the file is not a known image
     */
    public static Dimension getImageDimension(File imgFile) throws IOException {
        BufferedImage image = ImageIO.read(imgFile);
        if (image == null) {
            throw new IIOException("Could not read input file as an image!");
        }
        return new Dimension(image.getWidth(), image.getHeight());
    }

}
