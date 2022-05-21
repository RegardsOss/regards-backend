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
package fr.cnes.regards.modules.order.domain.basket;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.urn.DataType;

import java.util.List;

/**
 * Data type selection (quilooks and/or raw data)
 * File types are from enum class DataType (from rs-dam/entities-domain)
 *
 * @author oroussel
 */
public enum DataTypeSelection {

    ALL(DataType.RAWDATA,
        DataType.QUICKLOOK_SD,
        DataType.QUICKLOOK_MD,
        DataType.QUICKLOOK_HD), QUICKLOOKS(DataType.QUICKLOOK_SD,
                                           DataType.QUICKLOOK_MD,
                                           DataType.QUICKLOOK_HD), RAWDATA(DataType.RAWDATA);

    private DataType[] fileTypes;

    DataTypeSelection(DataType... fileTypes) {
        this.fileTypes = fileTypes;
    }

    public List<DataType> getFileTypes() {
        return Lists.newArrayList(this.fileTypes);
    }
}
