/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.dto.availability;

import fr.cnes.regards.modules.filecatalog.dto.availability.FileAvailabilityStatusDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collection;
import java.util.Objects;

/**
 * GDH format for availability status of one product Regards.
 *
 * @author Thomas GUILLOU
 **/
public class ProductFilesStatusDto {

    @Schema(description = "product id")
    private final String id;

    @Schema(description = "list of availability status of files link to the product")
    private final Collection<FileAvailabilityStatusDto> files;

    public ProductFilesStatusDto(String productId, Collection<FileAvailabilityStatusDto> fileStatuses) {
        this.id = productId;
        this.files = fileStatuses;
    }

    public String getId() {
        return id;
    }

    public Collection<FileAvailabilityStatusDto> getFiles() {
        return files;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProductFilesStatusDto that = (ProductFilesStatusDto) o;
        return Objects.equals(id, that.id) && Objects.equals(files, that.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, files);
    }

    @Override
    public String toString() {
        return "ProductFilesStatusDTO{" + "aipId='" + id + '\'' + ", fileStatuses=" + files + '}';
    }
}
