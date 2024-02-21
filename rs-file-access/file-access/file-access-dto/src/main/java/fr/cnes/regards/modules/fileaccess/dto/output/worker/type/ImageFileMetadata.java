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

import java.beans.ConstructorProperties;
import java.util.Objects;

/**
 * Specific metadata describing a stored image.
 *
 * @author Iliana Ghazali
 **/
public class ImageFileMetadata extends FileMetadata {

    /**
     * Height of the file in pixels
     */
    private final int heightInPx;

    /**
     * Width of the file in pixels
     */
    private final int widthInPx;

    @ConstructorProperties({ "checksum", "algorithm", "storedFileUrl", "fileSizeInBytes", "heightInPx", "widthInPx" })
    public ImageFileMetadata(String checksum,
                             String algorithm,
                             String storedFileUrl,
                             long fileSizeInBytes,
                             int heightInPx,
                             int widthInPx) {
        super(checksum, algorithm, storedFileUrl, fileSizeInBytes);
        this.heightInPx = heightInPx;
        this.widthInPx = widthInPx;
    }

    public int getHeightInPx() {
        return heightInPx;
    }

    public int getWidthInPx() {
        return widthInPx;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ImageFileMetadata that = (ImageFileMetadata) o;
        return heightInPx == that.heightInPx && widthInPx == that.widthInPx;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), heightInPx, widthInPx);
    }

    @Override
    public String toString() {
        return "ImageFileMetadata{" + "heightInPx=" + heightInPx + ", widthInPx=" + widthInPx + "} " + super.toString();
    }
}
