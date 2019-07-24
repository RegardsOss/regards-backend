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
package fr.cnes.regards.modules.order.domain.basket;

import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.SortNatural;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 * Represents an order basket.
 * It contains a list of datasets and for each a list of dated selections of items (a selection is an OpenSearch
 * request)
 * @author oroussel
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_basket", uniqueConstraints = @UniqueConstraint(name = "uk_basket_owner", columnNames = "owner"))
@NamedEntityGraph(name = "graph.basket",
        attributeNodes = @NamedAttributeNode(value = "datasetSelections", subgraph = "graph.basket.datasetSelections"),
        subgraphs = @NamedSubgraph(name = "graph.basket.datasetSelections",
                attributeNodes = @NamedAttributeNode("itemsSelections")))
public class Basket implements IIdentifiable<Long> {

    @Id
    @SequenceGenerator(name = "basketSequence", sequenceName = "seq_basket")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "basketSequence")
    private Long id;

    @Column(name = "owner", length = 100, nullable = false)
    private String owner;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "basket_id", foreignKey = @ForeignKey(name = "fk_dataset_selection"))
    @SortNatural
    private final SortedSet<BasketDatasetSelection> datasetSelections = new TreeSet<>();

    public Basket() {
    }

    public Basket(String owner) {
        this.owner = owner;
    }

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

    public SortedSet<BasketDatasetSelection> getDatasetSelections() {
        return datasetSelections;
    }

    public void addDatasetSelection(BasketDatasetSelection datasetSelections) {
        this.datasetSelections.add(datasetSelections);
    }

    public void removeDatasetSelection(BasketDatasetSelection datasetSelections) {
        this.datasetSelections.remove(datasetSelections);
    }
}
