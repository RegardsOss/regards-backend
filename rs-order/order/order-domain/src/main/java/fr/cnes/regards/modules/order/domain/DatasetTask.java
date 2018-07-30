/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Comparator;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.Valid;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractReliantTask;
import fr.cnes.regards.modules.order.domain.basket.BasketSelectionRequest;

/**
 * Dataset specific order task. This task is linked to optional processing task and to all sub-orders (files tasks) of
 * this dataset
 * @author oroussel
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_dataset_task")
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
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
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", name = "selection_requests")
    private final List<BasketSelectionRequest> selectionRequests = Lists.newArrayList();

    @Column(name = "objects_count")
    private int objectsCount = 0;

    @Column(name = "files_count")
    private int filesCount = 0;

    @Column(name = "files_size")
    private long filesSize = 0;

    // To be defined : a ProcessingTask should certainly be better
    // Or directly specifying JobInfo managing processing task
    @Column(name = "processing_service")
    @Type(type = "text")
    private String processingService;

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

    public int getFilesCount() {
        return filesCount;
    }

    public void setFilesCount(int filesCount) {
        this.filesCount = filesCount;
    }

    public long getFilesSize() {
        return filesSize;
    }

    public void setFilesSize(long filesSize) {
        this.filesSize = filesSize;
    }

    public String getProcessingService() {
        return processingService;
    }

    public void setProcessingService(String processingService) {
        this.processingService = processingService;
    }

    @Override
    public int compareTo(DatasetTask o) {
        return COMPARATOR.compare(this, o);
    }

    public void addSelectionRequest(BasketSelectionRequest selectionRequest) {
        selectionRequests.add(selectionRequest);
    }
}
