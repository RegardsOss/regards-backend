package fr.cnes.regards.modules.order.domain.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.BeanUtils;

import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.Order;

/**
 * Order Dto used to avoid loading FilesTask and all files
 * @author oroussel
 */
public class OrderDto {
    private Long id;

    private String owner;

    private UUID uid;

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

    public UUID getUid() {
        return uid;
    }

    public void setUid(UUID uid) {
        this.uid = uid;
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
