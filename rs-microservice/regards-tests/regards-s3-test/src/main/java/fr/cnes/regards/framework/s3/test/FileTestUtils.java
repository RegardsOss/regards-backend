/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.s3.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author Stephane Cortine
 */
public class FileTestUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileTestUtils.class);

    public static long getFileSize(URL sourceUrl) {
        long fileSize = 0l;
        URLConnection urlConnection = null;
        try {
            try {
                urlConnection = sourceUrl.openConnection();
                fileSize = urlConnection.getContentLengthLong();
            } finally {
                if (urlConnection != null) {
                    urlConnection.getInputStream().close();
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failure in the getting of file size : {}", sourceUrl, e);
        }
        return fileSize;
    }

}
