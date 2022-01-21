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
package fr.cnes.regards.modules.order.domain.dto;

import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.order.domain.basket.BasketDatedItemsSelection;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;
import fr.cnes.regards.modules.order.domain.basket.DataTypeSelection;

import java.time.OffsetDateTime;

public class BasketDatedItemsSelectionDto implements Comparable<BasketDatedItemsSelectionDto> {

    private OffsetDateTime date;

    private BasketSelectionRequest selectionRequest;

    private int objectsCount;

    private long filesCount;

    private long filesSize;

    private long quota;

    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime date) {
        this.date = date;
    }

    public BasketSelectionRequest getSelectionRequest() {
        return selectionRequest;
    }

    public void setSelectionRequest(BasketSelectionRequest selectionRequest) {
        this.selectionRequest = selectionRequest;
    }

    public int getObjectsCount() {
        return objectsCount;
    }

    public void setObjectsCount(int objectsCount) {
        this.objectsCount = objectsCount;
    }

    public long getFilesCount() {
        return filesCount;
    }

    public void setFilesCount(long filesCount) {
        this.filesCount = filesCount;
    }

    public long getFilesSize() {
        return filesSize;
    }

    public void setFilesSize(long filesSize) {
        this.filesSize = filesSize;
    }

    public long getQuota() {
        return quota;
    }

    public void setQuota(long quota) {
        this.quota = quota;
    }

    public static BasketDatedItemsSelectionDto makeBasketDatedItemsSelectionDto(BasketDatedItemsSelection basketDatedItemsSelection) {
        BasketDatedItemsSelectionDto dto = new BasketDatedItemsSelectionDto();
        dto.setDate(basketDatedItemsSelection.getDate());
        dto.setSelectionRequest(basketDatedItemsSelection.getSelectionRequest());
        dto.setObjectsCount(basketDatedItemsSelection.getObjectsCount());
        dto.setFilesCount(
            DataTypeSelection.ALL.getFileTypes().stream()
                .mapToLong(ft -> basketDatedItemsSelection.getFileTypeCount(ft.name()))
                .sum()
        );
        dto.setFilesSize(
            DataTypeSelection.ALL.getFileTypes().stream()
                .mapToLong(ft -> basketDatedItemsSelection.getFileTypeSize(ft.name()))
                .sum());
        dto.setQuota(basketDatedItemsSelection.getFileTypeCount(DataType.RAWDATA.name()+"_!ref"));
        return dto;
    }

    @Override
    public int compareTo(BasketDatedItemsSelectionDto o) {
        return date.compareTo(o.date);
    }
}
