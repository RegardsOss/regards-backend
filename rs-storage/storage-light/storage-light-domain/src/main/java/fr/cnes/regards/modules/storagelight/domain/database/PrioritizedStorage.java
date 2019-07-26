package fr.cnes.regards.modules.storagelight.domain.database;

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

import fr.cnes.regards.framework.module.manager.ConfigIgnore;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageType;

/**
 * Wrapper used to prioritize {@link fr.cnes.regards.modules.IStorageLocation.domain.plugin.IDataStorage} configurations.
 * As a wrapper, its database identifier is the same than the wrapped {@link PluginConfiguration}.
 * This wrapper is strictly ordered on priority.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Entity
@Table(name = "t_prioritized_storage", uniqueConstraints = { @UniqueConstraint(name = "uk_priotitized_storage",
        columnNames = { PrioritizedStorage.STORAGE_TYPE_COLUMN_NAME, PrioritizedStorage.PRIORITY_COLUMN_NAME }) })
public class PrioritizedStorage implements Comparable<PrioritizedStorage> {

    public static final String STORAGE_TYPE_COLUMN_NAME = "storage_type";

    public static final String PRIORITY_COLUMN_NAME = "priority";

    public static final long HIGHEST_PRIORITY = 0L;

    @Id
    @ConfigIgnore
    private Long id;

    @Valid
    @OneToOne
    @MapsId
    @JoinColumn(name = "storage_conf_id", foreignKey = @ForeignKey(name = "fk_prioritized_storage_plugin_conf"))
    private PluginConfiguration storageConfiguration;

    @Enumerated(EnumType.STRING)
    @Column(name = STORAGE_TYPE_COLUMN_NAME)
    private StorageType storageType;

    /**
     * Priority of this data storage.
     */
    @Min(HIGHEST_PRIORITY)
    @Column(name = PRIORITY_COLUMN_NAME)
    private Long priority;

    /**
     * Default constructor to be used only by serialization process or JPA
     */
    @SuppressWarnings("unused")
    private PrioritizedStorage() {
    }

    public PrioritizedStorage(PluginConfiguration dataStorageConfiguration, Long priority,
            StorageType dataStorageType) {
        this.storageConfiguration = dataStorageConfiguration;
        this.priority = priority;
        this.storageType = dataStorageType;
    }

    public PluginConfiguration getDataStorageConfiguration() {
        return storageConfiguration;
    }

    public void setDataStorageConfiguration(PluginConfiguration dataStorageConfiguration) {
        this.storageConfiguration = dataStorageConfiguration;
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

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        PrioritizedStorage that = (PrioritizedStorage) o;

        return storageConfiguration != null ? storageConfiguration.equals(that.storageConfiguration)
                : that.storageConfiguration == null;
    }

    @Override
    public int hashCode() {
        return storageConfiguration != null ? storageConfiguration.hashCode() : 0;
    }

    @Override
    public int compareTo(PrioritizedStorage o) {
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
