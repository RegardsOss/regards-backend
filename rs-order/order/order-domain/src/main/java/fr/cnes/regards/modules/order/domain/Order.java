/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedEntityGraphs;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.SortNatural;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;

/**
 * An order built from a basket
 * @author oroussel
 */
@Entity
@Table(name = "t_order")
@NamedEntityGraphs({ @NamedEntityGraph(name = "graph.order.complete",
        attributeNodes = @NamedAttributeNode(value = "datasetTasks", subgraph = "graph.order.complete.datasetTasks"),
        subgraphs = { @NamedSubgraph(name = "graph.order.complete.datasetTasks",
                attributeNodes = @NamedAttributeNode(value = "reliantTasks")) }),
        @NamedEntityGraph(name = "graph.order.simple", attributeNodes = @NamedAttributeNode(value = "datasetTasks")) })
public class Order implements IIdentifiable<Long>, Comparable<Order> {

    @Id
    @SequenceGenerator(name = "orderSequence", sequenceName = "seq_order")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orderSequence")
    private Long id;

    @Column(name = "owner", length = 100, nullable = false)
    private String owner;

    @Column(name = "creation_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime creationDate;

    @Column(name = "expiration_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime expirationDate;

    /**
     * Math.floor() percent completed task (number of treated files sizes on total files sizes)
     */
    @Column(name = "percent_complete", nullable = false)
    private int percentCompleted = 0;

    @Column(name = "files_in_error", nullable = false)
    private int filesInErrorCount = 0;

    @Column(name = "status", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "status_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime statusDate = OffsetDateTime.now();

    /**
     * To be downloaded files count
     */
    @Column(name = "available_count", nullable = false)
    private int availableFilesCount = 0;

    @Column(name = "avail_count_update_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime availableUpdateDate;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id", foreignKey = @ForeignKey(name = "fk_order"))
    @SortNatural
    private final SortedSet<DatasetTask> datasetTasks = new TreeSet<>(Comparator.naturalOrder());

    @Column(name = "waiting_for_user", nullable = false)
    private boolean waitingForUser = false;

    /**
     * URL provided by frontend when order is created and that permits to directly access order page
     */
    @Column(name = "url", columnDefinition = "text")
    private String frontendUrl;

    @Override
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

    public SortedSet<DatasetTask> getDatasetTasks() {
        return datasetTasks;
    }

    public void addDatasetOrderTask(DatasetTask datasetTask) {
        this.datasetTasks.add(datasetTask);
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

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
        this.statusDate = OffsetDateTime.now();
    }

    public OffsetDateTime getStatusDate() {
        return statusDate;
    }

    public int getAvailableFilesCount() {
        return availableFilesCount;
    }

    public void setAvailableFilesCount(int availableFilesCount) {
        this.availableFilesCount = availableFilesCount;
        this.availableUpdateDate = OffsetDateTime.now();
    }

    /**
     * This method should mostly not be used except when available update date must ONLY be set (and not
     * availableFilesCount.<br/>
     * @see #setAvailableFilesCount(int) should be used instead
     */
    public void setAvailableUpdateDate(OffsetDateTime availableUpdateDate) {
        this.availableUpdateDate = availableUpdateDate;
    }

    public OffsetDateTime getAvailableUpdateDate() {
        return availableUpdateDate;
    }

    public boolean isWaitingForUser() {
        return waitingForUser;
    }

    public void setWaitingForUser(boolean waitingForUser) {
        this.waitingForUser = waitingForUser;
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public int compareTo(Order o) {
        return this.creationDate.compareTo(o.getCreationDate());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        Order order = (Order) o;

        if (!owner.equals(order.owner)) {
            return false;
        }
        return creationDate.equals(order.creationDate);
    }

    @Override
    public int hashCode() {
        int result = owner.hashCode();
        result = (31 * result) + creationDate.hashCode();
        return result;
    }
}
