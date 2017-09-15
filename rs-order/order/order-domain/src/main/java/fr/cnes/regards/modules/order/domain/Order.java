package fr.cnes.regards.modules.order.domain;

import javax.persistence.AssociationOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
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
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

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
                                     attributeNodes = @NamedAttributeNode(value = "reliantTasks"))} ),
        @NamedEntityGraph(name = "graph.order.simple", attributeNodes = @NamedAttributeNode(value = "datasetTasks")) })
public class Order implements IIdentifiable<Long>, Comparable<Order> {

    @Id
    @SequenceGenerator(name = "orderSequence", sequenceName = "seq_order")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orderSequence")
    private Long id;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    /**
     * Unique identifier generated when creating order
     */
    @Column(name = "uid")
    private UUID uid = UUID.randomUUID();

    @Column(name = "creation_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime creationDate;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id", foreignKey = @ForeignKey(name = "fk_order"))
    @SortNatural
    private SortedSet<DatasetTask> datasetTasks = new TreeSet<>(Comparator.naturalOrder());

    public Order() {
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UUID getUid() {
        return uid;
    }

    //    private void setUid(UUID uid) {
    //        this.uid = uid;
    //    }

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

    @Override
    public int compareTo(Order o) {
        return this.creationDate.compareTo(o.getCreationDate());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Order order = (Order) o;

        if (!email.equals(order.email)) {
            return false;
        }
        return creationDate.equals(order.creationDate);
    }

    @Override
    public int hashCode() {
        int result = email.hashCode();
        result = 31 * result + creationDate.hashCode();
        return result;
    }
}
