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
package fr.cnes.regards.modules.order.domain.basket;

import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.SortNatural;

import fr.cnes.regards.framework.jpa.IIdentifiable;

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
    private int filesCount = 0;

    @Column(name = "files_size")
    private long filesSize = 0;

    @ElementCollection
    @CollectionTable(name = "t_basket_ds_item", joinColumns = @JoinColumn(name = "basket_dataset_id"),
            foreignKey = @ForeignKey(name = "fk_items_selection"))
    @SortNatural
    private final SortedSet<BasketDatedItemsSelection> itemsSelections = new TreeSet<>();

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

    public int getFilesCount() {
        return filesCount;
    }

    public void setFilesCount(int filesCount) {
        this.filesCount = filesCount;
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
}
