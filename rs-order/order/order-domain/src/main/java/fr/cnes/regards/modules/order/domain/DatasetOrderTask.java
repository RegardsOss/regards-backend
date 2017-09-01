package fr.cnes.regards.modules.order.domain;

import javax.persistence.Column;

import org.hibernate.annotations.Type;

/**
 * @author oroussel
 */
public class DatasetOrderTask extends OrderTask {
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

    // To be defined
    @Column(name = "processing_service")
    @Type(type = "text")
    private String processingService;

    public DatasetOrderTask() {
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
}
