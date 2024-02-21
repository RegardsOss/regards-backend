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
package fr.cnes.regards.modules.fileaccess.dto.output.worker.type;

import org.springframework.lang.Nullable;

import java.beans.ConstructorProperties;
import java.util.Objects;

/**
 * Generic processing information provided by the workers.
 *
 * @author Iliana Ghazali
 **/
public class FileProcessingMetadata {

    /**
     * Information to forward to the caller to indicate if small file management has been used by the worker.
     */
    private final boolean smallFileManaged;

    /**
     * Path to the temporarily cached file. Not null if smallFileManaged is true.
     */
    private final String cachePath;

    /**
     * Name of the file depending on the selected file name strategy.
     */
    private final String filename;

    /**
     * Parent url of the file to store. If smallFileManaged is true, the url represents the future parent location of
     * the file. By convention, this url ends with a trailing slash.
     */
    private final String storeParentUrl;

    /**
     * Parent path of the url. If the url is a s3 url, the path does not contain the bucket. By convention, this url
     * starts with a slash and ends with a trailing slash.
     */
    private final String storeParentPath;

    @ConstructorProperties({ "smallFileManaged", "cachePath", "filename", "storeParentUrl", "storeParentPath" })
    public FileProcessingMetadata(boolean smallFileManaged,
                                  @Nullable String cachePath,
                                  String filename,
                                  String storeParentUrl,
                                  String storeParentPath) {
        this.smallFileManaged = smallFileManaged;
        this.cachePath = cachePath;
        this.filename = filename;
        this.storeParentUrl = storeParentUrl;
        this.storeParentPath = storeParentPath;
    }

    public boolean isSmallFileManaged() {
        return smallFileManaged;
    }

    public String getCachePath() {
        return cachePath;
    }

    public String getFilename() {
        return filename;
    }

    public String getStoreParentUrl() {
        return storeParentUrl;
    }

    public String getStoreParentPath() {
        return storeParentPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileProcessingMetadata that = (FileProcessingMetadata) o;
        return smallFileManaged == that.smallFileManaged && Objects.equals(cachePath, that.cachePath) && Objects.equals(
            filename,
            that.filename) && Objects.equals(storeParentUrl, that.storeParentUrl) && Objects.equals(storeParentPath,
                                                                                                    that.storeParentPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(smallFileManaged, cachePath, filename, storeParentUrl, storeParentPath);
    }

    @Override
    public String toString() {
        return "FileProcessingMetadata{"
               + "smallFileManaged="
               + smallFileManaged
               + ", cachePath='"
               + cachePath
               + '\''
               + ", filename='"
               + filename
               + '\''
               + ", storeParentUrl='"
               + storeParentUrl
               + '\''
               + ", storeParentPath='"
               + storeParentPath
               + '\''
               + '}';
    }
}
