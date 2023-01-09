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
package fr.cnes.regards.modules.order.dto.input;

import fr.cnes.regards.framework.urn.DataType;

/**
 * This class contains all enum values of {@link fr.cnes.regards.framework.urn.DataType},
 * but without THUMBNAIL_SD, THUMBNAIL_MD, THUMBNAIL_HD.
 * Instead, we use THUMBNAIL.
 *
 * @author Thomas GUILLOU
 **/
public enum DataTypeLight {
    /**
     * Available data types
     */
    RAWDATA,
    QUICKLOOK,
    DOCUMENT,
    THUMBNAIL,
    OTHER,
    AIP,
    DESCRIPTION;

    public boolean isEquivalent(DataType dataType) {
        if (dataType == null) {
            return false;
        }
        return switch (dataType) {
            case QUICKLOOK_SD, QUICKLOOK_MD, QUICKLOOK_HD -> this == QUICKLOOK;
            default -> dataType.toString().equals(this.toString());
        };
    }
}