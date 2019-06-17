/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.domain;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MimeType;

/**
 * POJO to represent a file inline or in the cache system that is ready to be downloaded.
 * @author sbinda
 */
public class DownloadableFile implements Closeable {

    /**
     * Class logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadableFile.class);

    /**
     * Stream to read file content
     */
    private final InputStream fileInputStream;

    /**
     * File size calculated from the file on disk.
     */
    private final Long realFileSize;

    private final String fileName;

    private final MimeType mimeType;

    public DownloadableFile(InputStream fileInputStream, Long realFileSize, String fileName, MimeType mediaType) {
        super();
        this.fileInputStream = fileInputStream;
        this.realFileSize = realFileSize;
        this.fileName = fileName;
        this.mimeType = mediaType;
    }

    /**
     * @return the fileInputStream
     */
    public InputStream getFileInputStream() {
        return fileInputStream;
    }

    /**
     * @return the realFileSize
     */
    public Long getRealFileSize() {
        return realFileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    @Override
    public void close() {
        if (this.fileInputStream != null) {
            try {
                this.fileInputStream.close();
            } catch (IOException e) {
                LOGGER.error("Error closing File input stream.", e);
            }
        }
    }

}
