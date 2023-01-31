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
package fr.cnes.regards.modules.ltamanager.dto.submission;

/**
 * Available Data types for LTA request files.
 * Those types are the same as {@link fr.cnes.regards.framework.urn.DataType} with an additional ZIP type.
 * The ZIP type, is only available on LTA requests. This type is transformed by the LTA process to existing data
 * types after extraction.
 *
 * @author Sébastien Binda
 **/
public enum LtaDataType {

    /**
     * Available data types
     */
    ZIP,
    RAWDATA,
    QUICKLOOK_SD,
    QUICKLOOK_MD,
    QUICKLOOK_HD,
    DOCUMENT,
    THUMBNAIL,
    OTHER,
    AIP,
    DESCRIPTION;

    @Override
    public String toString() {
        return this.name();
    }

    public static LtaDataType parse(String value, LtaDataType defaultValue) {
        LtaDataType dt = defaultValue;
        if (value != null && valueOf(value) != null) {
            dt = valueOf(value);
        }
        return dt;
    }

}
