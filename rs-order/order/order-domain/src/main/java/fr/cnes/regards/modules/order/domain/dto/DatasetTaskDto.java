package fr.cnes.regards.modules.order.domain.dto;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.PropertyAccessorUtils;

import fr.cnes.regards.modules.order.domain.DatasetTask;

/**
 * DatasetTask Dto to avoid loading FilesTask and all associted files
 * @author oroussel
 */
public class DatasetTaskDto {
    private Long id;

    private String datasetLabel;

    private int objectsCount = 0;

    private int filesCount = 0;

    private long filesSize = 0;

    // To be defined : a ProcessingTask should certainly be better
    // Or directly specifying JobInfo managing processing task
    private String processingService;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    /**
     * Create DatasetTaskDto from DatasetTask
     */
    public static DatasetTaskDto fromDatasetTask(DatasetTask dsTask) {
        DatasetTaskDto dto = new DatasetTaskDto();
        BeanUtils.copyProperties(dsTask, dto);
        return dto;
    }
}
