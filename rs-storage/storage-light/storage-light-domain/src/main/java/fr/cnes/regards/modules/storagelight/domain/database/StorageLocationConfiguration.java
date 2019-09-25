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
import fr.cnes.regards.modules.storagelight.domain.plugin.IStorageLocation;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageType;

/**
 * Wrapper used to prioritize {@link IStorageLocation} configurations.
 * As a wrapper, its database identifier is the same than the wrapped {@link PluginConfiguration}.
 * This wrapper is strictly ordered on priority.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Entity
@Table(name = "t_storage_location_conf",
        uniqueConstraints = { @UniqueConstraint(name = "uk_priotitized_storage",
                columnNames = { StorageLocationConfiguration.STORAGE_TYPE_COLUMN_NAME,
                        StorageLocationConfiguration.PRIORITY_COLUMN_NAME }) })
public class StorageLocationConfiguration implements Comparable<StorageLocationConfiguration> {

    public static final String STORAGE_TYPE_COLUMN_NAME = "storage_type";

    public static final String PRIORITY_COLUMN_NAME = "priority";

    public static final long HIGHEST_PRIORITY = 0L;

    @Id
    @ConfigIgnore
    private Long id;

    @Valid
    @OneToOne
    @MapsId
    @JoinColumn(name = "plugin_conf_id", foreignKey = @ForeignKey(name = "fk_prioritized_storage_plugin_conf"))
    private PluginConfiguration pluginConfiguration;

    @Enumerated(EnumType.STRING)
    @Column(name = STORAGE_TYPE_COLUMN_NAME)
    private StorageType storageType;

    /**
     * Priority of this data storage.
     */
    @Min(HIGHEST_PRIORITY)
    @Column(name = PRIORITY_COLUMN_NAME)
    private Long priority;

    @Column(name = "allocated_size_ko")
    private Long allocatedSizeInKo;

    /**
     * Default constructor to be used only by serialization process or JPA
     */
    @SuppressWarnings("unused")
    private StorageLocationConfiguration() {
    }

    public StorageLocationConfiguration(PluginConfiguration dataStorageConfiguration, Long priority,
            Long allocatedSizeInKo, StorageType dataStorageType) {
        this.pluginConfiguration = dataStorageConfiguration;
        this.priority = priority;
        this.storageType = dataStorageType;
    }

    public PluginConfiguration getPluginConfiguration() {
        return pluginConfiguration;
    }

    public void setPluginConfiguration(PluginConfiguration dataStorageConfiguration) {
        this.pluginConfiguration = dataStorageConfiguration;
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

    public Long getAllocatedSizeInKo() {
        return allocatedSizeInKo;
    }

    public void setAllocatedSizeInKo(Long allocatedSizeInKo) {
        this.allocatedSizeInKo = allocatedSizeInKo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        StorageLocationConfiguration that = (StorageLocationConfiguration) o;

        return pluginConfiguration != null ? pluginConfiguration.equals(that.pluginConfiguration)
                : that.pluginConfiguration == null;
    }

    @Override
    public int hashCode() {
        return pluginConfiguration != null ? pluginConfiguration.hashCode() : 0;
    }

    @Override
    public int compareTo(StorageLocationConfiguration o) {
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
