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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.util.Assert;

import java.beans.ConstructorProperties;
import java.util.Objects;

/**
 * Information to identify a stored file.
 *
 * @author Iliana Ghazali
 **/
// annotation required to deserialize object with polymorphism
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
public class FileMetadata {

    /**
     * Computed file checksum
     */
    protected final String checksum;

    /**
     * Standard digest algorithm name from the "Java Cryptography Architecture Standard Algorithm Name Documentation"
     */
    protected final String algorithm;

    /**
     * Location of the stored file. It can be located on any type of storage server.
     */
    protected final String storedFileUrl;

    /**
     * Computed file size.
     */
    protected final long fileSizeInBytes;

    @ConstructorProperties({ "checksum", "algorithm", "storedFileUrl", "fileSizeInBytes" })
    public FileMetadata(String checksum, String algorithm, String storedFileUrl, long fileSizeInBytes) {
        Assert.notNull(checksum, "file checksum is mandatory!");
        Assert.notNull(algorithm, "file algorithm is mandatory!");

        this.checksum = checksum;
        this.algorithm = algorithm;
        this.storedFileUrl = storedFileUrl;
        this.fileSizeInBytes = fileSizeInBytes;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getStoredFileUrl() {
        return storedFileUrl;
    }

    public long getFileSizeInBytes() {
        return fileSizeInBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileMetadata that = (FileMetadata) o;
        return fileSizeInBytes == that.fileSizeInBytes && Objects.equals(checksum, that.checksum) && Objects.equals(
            algorithm,
            that.algorithm) && Objects.equals(storedFileUrl, that.storedFileUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checksum, algorithm, storedFileUrl, fileSizeInBytes);
    }

    @Override
    public String toString() {
        return "FileMetadata{"
               + "checksum='"
               + checksum
               + '\''
               + ", algorithm='"
               + algorithm
               + '\''
               + ", storedFileUrl="
               + storedFileUrl
               + ", fileSizeInBytes="
               + fileSizeInBytes
               + '}';
    }
}
