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
package fr.cnes.regards.modules.order.dto.input;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.Set;

/**
 * Filters to select files to order. Linked to {@link OrderRequestDto}
 *
 * @author Iliana Ghazali
 **/
public class OrderRequestFilters {

    /**
     * File types to order
     */
    @NotEmpty(message = "There should be at least one datatype to order. DataType can be RAWDATA or QUICKLOOK")
    private final Set<DataTypeLight> dataTypes;

    /**
     * Regexp to filter filenames
     */
    @Nullable
    private final String filenameRegExp;

    public OrderRequestFilters(Set<DataTypeLight> dataTypes, @Nullable String filenameRegExp) {
        Assert.notEmpty(dataTypes, "at least one dataType is mandatory");

        this.dataTypes = dataTypes;
        this.filenameRegExp = filenameRegExp;
    }

    public Set<DataTypeLight> getDataTypes() {
        return dataTypes;
    }

    @Nullable
    public String getFilenameRegExp() {
        return filenameRegExp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderRequestFilters that = (OrderRequestFilters) o;
        return dataTypes.equals(that.dataTypes) && Objects.equals(filenameRegExp, that.filenameRegExp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataTypes, filenameRegExp);
    }

    @Override
    public String toString() {
        return "OrderRequestFilters{" + "dataTypes=" + dataTypes + ", filenameRegExp='" + filenameRegExp + '\'' + '}';
    }
}
