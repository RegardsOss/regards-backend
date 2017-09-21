package fr.cnes.regards.modules.order.domain.basket;

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

import java.util.SortedSet;

import org.hibernate.annotations.SortNatural;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 * Represents an order basket.
 * It contains a list of datasets and for each a list of dated selections of items (a selection is an OpenSearch
 * request)
 * @author oroussel
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

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "basket_id", foreignKey = @ForeignKey(name = "fk_dataset_selection"))
    @SortNatural
    private SortedSet<BasketDatasetSelection> datasetSelections;

    @Override
    public Long getId() {
        return id;
    }

    public Basket() {
    }

    public Basket(String owner) {
        this.owner = owner;
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

    public void setDatasetSelections(SortedSet<BasketDatasetSelection> datasetSelections) {
        this.datasetSelections = datasetSelections;
    }
}
