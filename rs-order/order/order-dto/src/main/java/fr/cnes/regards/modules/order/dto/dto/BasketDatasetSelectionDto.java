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
package fr.cnes.regards.modules.order.dto.dto;

import java.util.SortedSet;

public class BasketDatasetSelectionDto implements Comparable<BasketDatasetSelectionDto> {

    private Long id;

    private String datasetIpid;

    private String datasetLabel;

    private int objectsCount;

    private long filesCount;

    private long filesSize;

    private long quota;

    private SortedSet<BasketDatedItemsSelectionDto> itemsSelections;

    private ProcessDatasetDescriptionDto processDatasetDescription;

    private FileSelectionDescriptionDto fileSelectionDescription;

    public FileSelectionDescriptionDto getFileSelectionDescription() {
        return fileSelectionDescription;
    }

    public void setFileSelectionDescription(FileSelectionDescriptionDto fileSelectionDescription) {
        this.fileSelectionDescription = fileSelectionDescription;
    }

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

    public ProcessDatasetDescriptionDto getProcessDatasetDescription() {
        return processDatasetDescription;
    }

    public void setProcessDatasetDescription(ProcessDatasetDescriptionDto processDatasetDescription) {
        this.processDatasetDescription = processDatasetDescription;
    }

    @Override
    public int compareTo(BasketDatasetSelectionDto o) {
        return datasetLabel.compareToIgnoreCase(o.datasetLabel);
    }
}
