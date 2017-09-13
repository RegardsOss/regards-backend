package fr.cnes.regards.modules.order.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import java.util.Comparator;

import org.hibernate.annotations.Type;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractReliantTask;

/**
 * Dataset specific order task. This task is linked to optional processing task and to all sub-orders (files tasks) of
 * this dataset
 * @author oroussel
 */
@Entity
@Table(name = "t_dataset_task")
@PrimaryKeyJoinColumn(foreignKey = @ForeignKey(name = "fk_task_id"))
@NamedEntityGraph(name = "graph.complete", attributeNodes = @NamedAttributeNode(value = "reliantTasks"))
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
    @Column(name = "opensearch_request")
    @Type(type = "text")
    private String openSearchRequest;

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

    public DatasetTask() {
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

    public String getOpenSearchRequest() {
        return openSearchRequest;
    }

    public void setOpenSearchRequest(String openSearchRequest) {
        this.openSearchRequest = openSearchRequest;
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
}
