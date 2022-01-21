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

import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderStatus;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Order Dto used to avoid loading FilesTask and all files
 * @author oroussel
 */
public class OrderDto {

    private Long id;

    private String owner;

    private String label;

    private OffsetDateTime creationDate;

    private OffsetDateTime expirationDate;

    private int percentCompleted;

    private int filesInErrorCount;

    private int availableFilesCount;

    private OrderStatus status;

    private OffsetDateTime statusDate;

    private boolean waitingForUser;

    private List<DatasetTaskDto> datasetTasks = new ArrayList<>();

    public OrderDto() {}

    public OrderDto(
            Long id,
            String owner,
            String label,
            OffsetDateTime creationDate,
            OffsetDateTime expirationDate,
            int percentCompleted,
            int filesInErrorCount,
            int availableFilesCount,
            OrderStatus status,
            OffsetDateTime statusDate,
            boolean waitingForUser,
            List<DatasetTaskDto> datasetTasks
    ) {
        this.id = id;
        this.owner = owner;
        this.label = label;
        this.creationDate = creationDate;
        this.expirationDate = expirationDate;
        this.percentCompleted = percentCompleted;
        this.filesInErrorCount = filesInErrorCount;
        this.availableFilesCount = availableFilesCount;
        this.status = status;
        this.statusDate = statusDate;
        this.waitingForUser = waitingForUser;
        this.datasetTasks = datasetTasks;
    }

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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public boolean isWaitingForUser() {
        return waitingForUser;
    }

    public void setWaitingForUser(boolean waitingForUser) {
        this.waitingForUser = waitingForUser;
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
        OrderDto dto = new OrderDto(
            order.getId(), order.getOwner(), order.getLabel(),
            order.getCreationDate(), order.getExpirationDate(),
            order.getPercentCompleted(),
            order.getFilesInErrorCount(),
            order.getAvailableFilesCount(),
            order.getStatus(), order.getStatusDate(),
            order.isWaitingForUser(),
            order.getDatasetTasks().stream()
                .map(DatasetTaskDto::fromDatasetTask)
                .collect(Collectors.toList())
        );
        return dto;
    }
}
