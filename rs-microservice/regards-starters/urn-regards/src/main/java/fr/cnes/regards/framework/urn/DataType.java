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
package fr.cnes.regards.framework.urn;

import jakarta.annotation.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Entity types
 *
 * @author lmieulet
 * @author Marc Sordi
 */
public enum DataType {

    /**
     * Available data types
     */
    RAWDATA, QUICKLOOK_SD, QUICKLOOK_MD, QUICKLOOK_HD, DOCUMENT, THUMBNAIL, OTHER, AIP, DESCRIPTION;

    @Override
    public String toString() {
        return this.name();
    }

    /**
     * Find the constant matching the given name.
     *
     * @param name the name of the constant to be found.
     * @return an Optional of the matching constant or an empty Optional if no enum constant is matching.
     */
    public static Optional<DataType> find(@Nullable String name) {
        Optional<DataType> found;
        if (name == null) {
            return Optional.empty();
        }

        return Stream.of(DataType.values()).filter(t -> t.name().equalsIgnoreCase(name)).findFirst();
    }

    public static DataType parse(String value, DataType defaultValue) {
        DataType dt = defaultValue;
        if (value != null) {
            // NullPointerException if no matching name
            // is-it really what we want?
            dt = valueOf(value);
        }
        return dt;
    }

}
