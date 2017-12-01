package fr.cnes.regards.modules.order.domain.dto;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.BeanUtils;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderStatus;

/**
 * Order Dto used to avoid loading FilesTask and all files
 * @author oroussel
 */
public class OrderDto {

    private Long id;

    private String owner;

    private OffsetDateTime creationDate;

    private OffsetDateTime expirationDate;

    private int percentCompleted;

    private int filesInErrorCount;

    private int availableFilesCount;

    private OrderStatus status;

    private OffsetDateTime statusDate;

    private List<DatasetTaskDto> datasetTasks = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(OffsetDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public int getPercentCompleted() {
        return percentCompleted;
    }

    public void setPercentCompleted(int percentCompleted) {
        this.percentCompleted = percentCompleted;
    }

    public int getFilesInErrorCount() {
        return filesInErrorCount;
    }

    public void setFilesInErrorCount(int filesInErrorCount) {
        this.filesInErrorCount = filesInErrorCount;
    }

    public int getAvailableFilesCount() {
        return availableFilesCount;
    }

    public void setAvailableFilesCount(int availableFilesCount) {
        this.availableFilesCount = availableFilesCount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public OffsetDateTime getStatusDate() {
        return statusDate;
    }

    public void setStatusDate(OffsetDateTime statusDate) {
        this.statusDate = statusDate;
    }

    public List<DatasetTaskDto> getDatasetTasks() {
        return datasetTasks;
    }

    public void setDatasetTasks(List<DatasetTaskDto> datasetTasks) {
        this.datasetTasks = datasetTasks;
    }

    /**
     * Create OrderDto from Order
     */
    public static OrderDto fromOrder(Order order) {
        OrderDto dto = new OrderDto();
        BeanUtils.copyProperties(order, dto, "datasetTasks");
        List<DatasetTaskDto> dsTaskDtos = new ArrayList<>();
        for (DatasetTask dsTask : order.getDatasetTasks()) {
            dsTaskDtos.add(DatasetTaskDto.fromDatasetTask(dsTask));
        }
        dto.setDatasetTasks(dsTaskDtos);
        return dto;
    }
}
