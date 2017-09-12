package fr.cnes.regards.modules.storage.domain.parameter;

import javax.persistence.*;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Entity
@Table(name = "t_storage_parameter")
public class StorageParameter {

    /**
     * name of the property defining at which rate the aip metadata should be updated on the different storages. In minutes.
     */
    public static final String UPDATE_RATE = "update_rate";

    @Id
    @SequenceGenerator(name = "storageParameterSequence", initialValue = 1, sequenceName = "storage_parameter_sequence")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "storageParameterSequence")
    private Long id;

    @Column(unique = true)
    private String name;

    @Column(columnDefinition = "text")
    private String value;

    public StorageParameter(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public StorageParameter() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
