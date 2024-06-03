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
package fr.cnes.regards.modules.order.domain.basket;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.order.dto.dto.BasketDatasetSelectionDto;
import fr.cnes.regards.modules.order.dto.dto.FileSelectionDescription;
import fr.cnes.regards.modules.order.dto.dto.FileSelectionDescriptionDTO;
import fr.cnes.regards.modules.order.dto.dto.ProcessDatasetDescription;
import org.hibernate.annotations.SortNatural;
import org.hibernate.annotations.Type;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A grouped items by dataset selection from a basket
 *
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
    private long filesCount = 0;

    @Column(name = "files_size")
    private long filesSize = 0;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", name = "file_types_sizes")
    private StringToLongMap fileTypesSizes;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", name = "file_types_count")
    private StringToLongMap fileTypesCount;

    @Nullable
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb", name = "file_selection_description")
    private FileSelectionDescription fileSelectionDescription;

    @ElementCollection
    @CollectionTable(name = "t_basket_ds_item",
                     joinColumns = @JoinColumn(name = "basket_dataset_id"),
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

    public long getFilesSize() {
        return filesSize;
    }

    public void setFilesSize(long filesSize) {
        this.filesSize = filesSize;
    }

    public long getFilesCount() {
        return filesCount;
    }

    public void setFilesCount(long filesCount) {
        this.filesCount = filesCount;
    }

    public Long getFileTypeSize(String fileType) {
        return Optional.ofNullable(fileTypesSizes).map(m -> m.getOrDefault(fileType, 0L)).orElse(0L);
    }

    public void setFileTypeSize(String fileType, Long filesSize) {
        if (fileTypesSizes == null) {
            fileTypesSizes = new StringToLongMap();
        }
        this.fileTypesSizes.put(fileType, filesSize);
    }

    public Long getFileTypeCount(String fileType) {
        return Optional.ofNullable(fileTypesCount).map(m -> m.getOrDefault(fileType, 0L)).orElse(0L);
    }

    public void setFileTypeCount(String fileType, Long filesCount) {
        if (fileTypesCount == null) {
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

    @Nullable
    public FileSelectionDescription getFileSelectionDescription() {
        return fileSelectionDescription;
    }

    public void setFileSelectionDescription(@Nullable FileSelectionDescription fileSelectionDescription) {
        this.fileSelectionDescription = fileSelectionDescription;
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

    public BasketDatasetSelectionDto toBasketDatasetSelectionDto() {
        BasketDatasetSelectionDto dto = new BasketDatasetSelectionDto();
        dto.setId(this.getId());
        dto.setDatasetIpid(this.getDatasetIpid());
        dto.setDatasetLabel(this.getDatasetLabel());
        dto.setObjectsCount(this.getObjectsCount());
        dto.setFilesCount(DataTypeSelection.ALL.getFileTypes()
                                               .stream()
                                               .mapToLong(ft -> this.getFileTypeCount(ft.name()))
                                               .sum());
        dto.setFilesSize(DataTypeSelection.ALL.getFileTypes()
                                              .stream()
                                              .mapToLong(ft -> this.getFileTypeSize(ft.name()))
                                              .sum());
        dto.setItemsSelections(this.getItemsSelections()
                                   .stream()
                                   .map(BasketDatedItemsSelection::toBasketDatedItemsSelectionDto)
                                   .collect(TreeSet::new, Set::add, TreeSet::addAll));
        dto.setQuota(this.getFileTypeCount(DataType.RAWDATA.name() + "_!ref"));
        dto.setProcessDatasetDescription(this.getProcessDatasetDescription());
        dto.setFileSelectionDescription(FileSelectionDescriptionDTO.makeFileSelectionDescriptionDTO(this.getFileSelectionDescription()));
        return dto;
    }

}
