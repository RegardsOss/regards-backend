/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.domain.basket;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.modules.order.domain.process.ProcessDatasetDescription;
import org.hibernate.annotations.SortNatural;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A grouped items by dataset selection from a basket
 * @author oroussel
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_basket_dataset")
public class BasketDatasetSelection implements IIdentifiable<Long>, Comparable<BasketDatasetSelection> {

    @Id
    @SequenceGenerator(name = "datasetItemsSelectionSequence", sequenceName = "seq_ds_items_sel")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "datasetItemsSelectionSequence")
    private Long id;

    @Column(name = "dataset_ip_id", length = 128, nullable = false)
    private String datasetIpid;

    @Column(name = "dataset_label", length = 128, nullable = false)
    private String datasetLabel;

    @Column(name = "objects_count")
    private int objectsCount = 0;

    @Column(name = "files_count")
    private final long filesCount = 0;

    @Column(name = "files_size")
    private final long filesSize = 0;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", name = "file_types_sizes")
    private StringToLongMap fileTypesSizes;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", name = "file_types_count")
    private StringToLongMap fileTypesCount;

    @ElementCollection
    @CollectionTable(name = "t_basket_ds_item", joinColumns = @JoinColumn(name = "basket_dataset_id"),
            foreignKey = @ForeignKey(name = "fk_items_selection"))
    @SortNatural
    private final SortedSet<BasketDatedItemsSelection> itemsSelections = new TreeSet<>();

    @Column(name = "process_dataset_desc")
    @Type(type = "jsonb")
    private ProcessDatasetDescription processDatasetDescription;

    @Override
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

    public long getFilesSize() {
        return filesSize;
    }

    public Long getFileTypeSize(String fileType) {
        return Optional.ofNullable(fileTypesSizes)
            .map(m -> m.getOrDefault(fileType, 0L))
            .orElse(0L);
    }

    public void setFileTypeSize(String fileType, Long filesSize) {
        if (fileTypesSizes==null) {
            fileTypesSizes = new StringToLongMap();
        }
        this.fileTypesSizes.put(fileType, filesSize);
    }

    public Long getFileTypeCount(String fileType) {
        return Optional.ofNullable(fileTypesCount)
            .map(m -> m.getOrDefault(fileType, 0L))
            .orElse(0L);
    }

    public void setFileTypeCount(String fileType, Long filesCount) {
        if (fileTypesCount==null) {
            fileTypesCount = new StringToLongMap();
        }
        this.fileTypesCount.put(fileType, filesCount);
    }

    public StringToLongMap getFileTypesSizes() {
        return fileTypesSizes;
    }

    public StringToLongMap getFileTypesCount() {
        return fileTypesCount;
    }

    public BasketDatasetSelection setFileTypesSizes(StringToLongMap fileTypesSizes) {
        this.fileTypesSizes = fileTypesSizes;
        return this;
    }

    public BasketDatasetSelection setFileTypesCount(StringToLongMap fileTypesCount) {
        this.fileTypesCount = fileTypesCount;
        return this;
    }

    public SortedSet<BasketDatedItemsSelection> getItemsSelections() {
        return itemsSelections;
    }

    public void addItemsSelection(BasketDatedItemsSelection itemsSelection) {
        this.itemsSelections.add(itemsSelection);
    }

    public void removeItemsSelection(BasketDatedItemsSelection itemsSelection) {
        this.itemsSelections.remove(itemsSelection);
    }

    public ProcessDatasetDescription getProcessDatasetDescription() {
        return processDatasetDescription;
    }

    public void setProcessDatasetDescription(ProcessDatasetDescription processDatasetDescription) {
        this.processDatasetDescription = processDatasetDescription;
    }

    @Override
    public int compareTo(BasketDatasetSelection o) {
        return datasetLabel.compareToIgnoreCase(o.datasetLabel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        BasketDatasetSelection that = (BasketDatasetSelection) o;

        return datasetIpid.equals(that.datasetIpid);
    }

    @Override
    public int hashCode() {
        return datasetIpid.hashCode();
    }

    public boolean hasProcessing() {
        return this.processDatasetDescription != null;
    }
}
