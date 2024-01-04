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

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

/**
 * GDH format for availability status responses of the product availability endpoint
 *
 * @author Thomas GUILLOU
 **/
public class FilesAvailabilityResponseDto {

    @Schema(description = "list of availability status of products")
    private final List<ProductFilesStatusDto> products;

    public FilesAvailabilityResponseDto(List<ProductFilesStatusDto> productStatus) {
        this.products = productStatus;
    }

    public List<ProductFilesStatusDto> getProducts() {
        return products;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FilesAvailabilityResponseDto that = (FilesAvailabilityResponseDto) o;
        return Objects.equals(products, that.products);
    }

    @Override
    public int hashCode() {
        return Objects.hash(products);
    }

    @Override
    public String toString() {
        return "FilesAvailabilityResponseDTO{" + "products=" + products + '}';
    }
}
