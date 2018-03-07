package fr.cnes.regards.modules.storage.domain.database;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.Valid;
import javax.validation.constraints.Min;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;

/**
 * Wrapper used to prioritize {@link fr.cnes.regards.modules.storage.domain.plugin.IDataStorage} configurations.
 * As a wrapper, its database identifier is the same than the wrapped {@link PluginConfiguration}.
 * This wrapper is strictly ordered on priority.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Entity
@Table(name = "t_prioritized_data_storage", uniqueConstraints = {
        @UniqueConstraint(name = "uk_priotitized_data_storage",
                columnNames = { PrioritizedDataStorage.DATA_STORAGE_TYPE_COLUMN_NAME,
                        PrioritizedDataStorage.PRIORITY_COLUMN_NAME }) })
public class PrioritizedDataStorage implements Comparable<PrioritizedDataStorage> {

    public static final String DATA_STORAGE_TYPE_COLUMN_NAME = "data_storage_type";

    public static final String PRIORITY_COLUMN_NAME = "priority";

    public static final long HIGHEST_PRIORITY = 0L;

    @Id
    private Long id;

    @Valid
    @OneToOne
    @MapsId
    @JoinColumn(name = "data_storage_conf_id",
            foreignKey = @ForeignKey(name = "fk_prioritized_data_storage_plugin_conf"))
    private PluginConfiguration dataStorageConfiguration;

    @Enumerated(EnumType.STRING)
    @Column(name = DATA_STORAGE_TYPE_COLUMN_NAME)
    private DataStorageType dataStorageType;

    /**
     * Priority of this data storage.
     */
    @Min(HIGHEST_PRIORITY)
    @Column(name = PRIORITY_COLUMN_NAME)
    private Long priority;

    /**
     * Default constructor to be used only by serialization process or JPA
     */
    private PrioritizedDataStorage() {
    }

    public PrioritizedDataStorage(PluginConfiguration dataStorageConfiguration, Long priority,
            DataStorageType dataStorageType) {
        this.dataStorageConfiguration = dataStorageConfiguration;
        this.priority = priority;
        this.dataStorageType = dataStorageType;
    }

    public PluginConfiguration getDataStorageConfiguration() {
        return dataStorageConfiguration;
    }

    public void setDataStorageConfiguration(PluginConfiguration dataStorageConfiguration) {
        this.dataStorageConfiguration = dataStorageConfiguration;
    }

    public Long getPriority() {
        return priority;
    }

    public void setPriority(Long priority) {
        this.priority = priority;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DataStorageType getDataStorageType() {
        return dataStorageType;
    }

    public void setDataStorageType(DataStorageType dataStorageType) {
        this.dataStorageType = dataStorageType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PrioritizedDataStorage that = (PrioritizedDataStorage) o;

        return dataStorageConfiguration != null ?
                dataStorageConfiguration.equals(that.dataStorageConfiguration) :
                that.dataStorageConfiguration == null;
    }

    @Override
    public int hashCode() {
        return dataStorageConfiguration != null ? dataStorageConfiguration.hashCode() : 0;
    }

    @Override
    public int compareTo(PrioritizedDataStorage o) {
        // we implement a strict order on priorities so just to keep coherence with equals,
        // if we compare to ourselves compareTo returns 0
        if (this.equals(o)) {
            return 0;
        }
        if (this.priority > o.priority) {
            return 1;
        } else {
            return -1;
        }
    }
}
