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
package fr.cnes.regards.modules.search.dto.availability;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.Objects;
import java.util.Set;

/**
 * payload of the product availability endpoint
 *
 * @author Thomas GUILLOU
 **/
public class FilesAvailabilityRequestDto {

    @NotEmpty
    @Schema(name = "product_ids", description = "list of urn of products")
    @SerializedName("product_ids")
    private final Set<String> productIds;

    public FilesAvailabilityRequestDto(Set<String> productIds) {
        this.productIds = productIds;
    }

    public Set<String> getProductIds() {
        return productIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FilesAvailabilityRequestDto that = (FilesAvailabilityRequestDto) o;
        return Objects.equals(productIds, that.productIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productIds);
    }

    @Override
    public String toString() {
        return "FileAvailabilityRequestDto{" + "productIds=" + productIds + '}';
    }
}
