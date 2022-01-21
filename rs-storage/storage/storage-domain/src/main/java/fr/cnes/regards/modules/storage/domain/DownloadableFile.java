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
package fr.cnes.regards.modules.storage.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MimeType;

import java.io.Closeable;
import java.io.InputStream;

/**
 * POJO to represent a file inline or in the cache system that is ready to be downloaded.<br/>
 *
 * @author Sébastien Binda
 */
public abstract class DownloadableFile implements Closeable {

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
    private final Long fileSize;

    private final String fileName;

    private final MimeType mimeType;

    protected DownloadableFile(InputStream fileInputStream, Long fileSize, String fileName, MimeType mediaType) {
        super();
        this.fileInputStream = fileInputStream;
        this.fileSize = fileSize;
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
        return fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public MimeType getMimeType() {
        return mimeType;
    }
}
