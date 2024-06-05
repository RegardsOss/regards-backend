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
package fr.cnes.regards.modules.order.domain;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractReliantTask;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.order.dto.dto.BasketSelectionRequest;
import fr.cnes.regards.modules.order.dto.dto.DatasetTaskDto;
import fr.cnes.regards.modules.order.dto.dto.FileSelectionDescription;
import fr.cnes.regards.modules.order.dto.dto.ProcessDatasetDescription;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import org.hibernate.annotations.Type;
import org.springframework.beans.BeanUtils;

import java.util.Comparator;
import java.util.List;

/**
 * Dataset specific order task. This task is linked to optional processing task and to all sub-orders (files tasks) of
 * this dataset
 *
 * @author oroussel
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_dataset_task")

@PrimaryKeyJoinColumn(foreignKey = @ForeignKey(name = "fk_task_id"))
@NamedEntityGraph(name = "graph.datasetTask.complete", attributeNodes = @NamedAttributeNode(value = "reliantTasks"))
public class DatasetTask extends AbstractReliantTask<FilesTask> implements Comparable<DatasetTask> {

    /**
     * Comparator by dataset label.
     */
    private static final Comparator<DatasetTask> COMPARATOR = Comparator.comparing(DatasetTask::getDatasetLabel);

    @Column(name = "dataset_ip_id", length = 128, nullable = false)
    private String datasetIpid;

    @Column(name = "dataset_label", length = 128, nullable = false)
    private String datasetLabel;

    /**
     * Selection request determined from BasketDatasetSelection
     */
    @Valid
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "selection_requests")
    private final List<BasketSelectionRequest> selectionRequests = Lists.newArrayList();

    @Column(name = "objects_count")
    private int objectsCount = 0;

    @Column(name = "files_count")
    private long filesCount = 0;

    @Column(name = "files_size")
    private long filesSize = 0;

    @Column(name = "process_dataset_desc")
    @Type(JsonBinaryType.class)
    private ProcessDatasetDescription processDatasetDescription;

    @Nullable
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "file_selection_description")
    private FileSelectionDescription fileSelectionDescription;

    public DatasetTask() {
    }

    public DatasetTask(String datasetIpid,
                       String datasetLabel,
                       long filesCount,
                       long filesSize,
                       int objectsCount,
                       List<BasketSelectionRequest> selectionRequests) {
        this.datasetIpid = datasetIpid;
        this.datasetLabel = datasetLabel;
        this.filesCount = filesCount;
        this.filesSize = filesSize;
        this.objectsCount = objectsCount;
        selectionRequests.addAll(selectionRequests);
    }

    public static DatasetTask fromBasketSelection(BasketDatasetSelection dsSel, List<DataType> orderdDataTypes) {
        DatasetTask dsTask = new DatasetTask();

        dsTask.setDatasetIpid(dsSel.getDatasetIpid());
        dsTask.setDatasetLabel(dsSel.getDatasetLabel());
        dsTask.setFilesCount(orderdDataTypes.stream().mapToLong(ft -> dsSel.getFileTypeCount(ft.name())).sum());
        dsTask.setFilesSize(orderdDataTypes.stream().mapToLong(ft -> dsSel.getFileTypeSize(ft.name())).sum());
        dsTask.setObjectsCount(dsSel.getObjectsCount());
        dsTask.setProcessDatasetDescription(dsSel.getProcessDatasetDescription());
        dsTask.setFileSelectionDescription(dsSel.getFileSelectionDescription());
        dsSel.getItemsSelections().forEach(item -> {
            dsTask.addSelectionRequest(item.getSelectionRequest());
        });

        return dsTask;
    }

    private void setFileSelectionDescription(@Nullable FileSelectionDescription fileSelectionDescription) {
        this.fileSelectionDescription = fileSelectionDescription;
    }

    @Nullable
    public FileSelectionDescription getFileSelectionDescription() {
        return fileSelectionDescription;
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

    public List<BasketSelectionRequest> getSelectionRequests() {
        return selectionRequests;
    }

    @Override
    public int compareTo(DatasetTask o) {
        return COMPARATOR.compare(this, o);
    }

    public void addSelectionRequest(BasketSelectionRequest selectionRequest) {
        selectionRequests.add(selectionRequest);
    }

    public boolean hasProcessing() {
        return this.processDatasetDescription != null;
    }

    public ProcessDatasetDescription getProcessDatasetDescription() {
        return processDatasetDescription;
    }

    public void setProcessDatasetDescription(ProcessDatasetDescription processDatasetDescription) {
        this.processDatasetDescription = processDatasetDescription;
    }

    /**
     * Create DatasetTaskDto from DatasetTask
     */
    public DatasetTaskDto toDatasetTaskDto() {
        DatasetTaskDto dto = new DatasetTaskDto();
        // FIXME avoid use copyProperties in production code
        BeanUtils.copyProperties(this, dto);
        return dto;
    }
}
