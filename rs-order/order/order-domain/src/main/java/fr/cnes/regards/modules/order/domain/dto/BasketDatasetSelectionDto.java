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
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.domain.basket.DataTypeSelection;
import fr.cnes.regards.modules.order.domain.process.ProcessDatasetDescription;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class BasketDatasetSelectionDto implements Comparable<BasketDatasetSelectionDto> {

    private Long id;

    private String datasetIpid;

    private String datasetLabel;

    private int objectsCount;

    private long filesCount;

    private long filesSize;

    private long quota;

    private SortedSet<BasketDatedItemsSelectionDto> itemsSelections;

    private ProcessDatasetDescription processDatasetDescription;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDatasetIpid() {
        return datasetIpid;
    }

    public void setDatasetIpid(String datasetIpid) {
        this.datasetIpid = datasetIpid;
    }

    public String getDatasetLabel() {
        return datasetLabel;
    }

    public void setDatasetLabel(String datasetLabel) {
        this.datasetLabel = datasetLabel;
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

    public SortedSet<BasketDatedItemsSelectionDto> getItemsSelections() {
        return itemsSelections;
    }

    public void setItemsSelections(SortedSet<BasketDatedItemsSelectionDto> itemsSelections) {
        this.itemsSelections = itemsSelections;
    }

    public ProcessDatasetDescription getProcessDatasetDescription() {
        return processDatasetDescription;
    }

    public void setProcessDatasetDescription(ProcessDatasetDescription processDatasetDescription) {
        this.processDatasetDescription = processDatasetDescription;
    }

    public static BasketDatasetSelectionDto makeBasketDatasetSelectionDto(BasketDatasetSelection basketDatasetSelection) {
        BasketDatasetSelectionDto dto = new BasketDatasetSelectionDto();
        dto.setId(basketDatasetSelection.getId());
        dto.setDatasetIpid(basketDatasetSelection.getDatasetIpid());
        dto.setDatasetLabel(basketDatasetSelection.getDatasetLabel());
        dto.setObjectsCount(basketDatasetSelection.getObjectsCount());
        dto.setFilesCount(DataTypeSelection.ALL.getFileTypes()
                                               .stream()
                                               .mapToLong(ft -> basketDatasetSelection.getFileTypeCount(ft.name()))
                                               .sum());
        dto.setFilesSize(DataTypeSelection.ALL.getFileTypes()
                                              .stream()
                                              .mapToLong(ft -> basketDatasetSelection.getFileTypeSize(ft.name()))
                                              .sum());
        dto.setItemsSelections(basketDatasetSelection.getItemsSelections()
                                                     .stream()
                                                     .map(BasketDatedItemsSelectionDto::makeBasketDatedItemsSelectionDto)
                                                     .collect(TreeSet::new, Set::add, TreeSet::addAll));
        dto.setQuota(basketDatasetSelection.getFileTypeCount(DataType.RAWDATA.name() + "_!ref"));
        dto.setProcessDatasetDescription(basketDatasetSelection.getProcessDatasetDescription());
        return dto;
    }

    @Override
    public int compareTo(BasketDatasetSelectionDto o) {
        return datasetLabel.compareToIgnoreCase(o.datasetLabel);
    }
}
