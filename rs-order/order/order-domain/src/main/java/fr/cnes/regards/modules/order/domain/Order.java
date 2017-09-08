package fr.cnes.regards.modules.order.domain;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.NamedSubgraph;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.annotations.SortNatural;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;

/**
 * An order built from a basket
 * @author oroussel
 */
@Entity
@Table(name = "t_order")
@NamedEntityGraph(name = "graph.order",
        attributeNodes = @NamedAttributeNode(value = "datasetTasks", subgraph = "graph.order.datasetTasks"),
        subgraphs = { @NamedSubgraph(name = "graph.order.datasetTasks",
                attributeNodes = @NamedAttributeNode(value = "reliantTasks")) })
public class Order implements IIdentifiable<Long>, Comparable<Order> {

    @Id
    @SequenceGenerator(name = "orderSequence", sequenceName = "seq_order")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orderSequence")
    private Long id;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime creationDate;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "ta_order_dataset_task", foreignKey = @ForeignKey(name = "fk_order"),
            inverseForeignKey = @ForeignKey(name = "fk_dataset_task"),
            inverseJoinColumns = @JoinColumn(name = "dataset_task_id"),
            uniqueConstraints = @UniqueConstraint(name = "uk_dataset_task_id",
                    columnNames = "dataset_task_id"))
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
