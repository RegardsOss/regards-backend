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
package fr.cnes.regards.modules.storage.domain.database;

import fr.cnes.regards.framework.module.manager.ConfigIgnore;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.filecatalog.dto.StorageLocationConfigurationDto;
import fr.cnes.regards.modules.filecatalog.dto.StorageType;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineStorageLocation;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineStorageLocation;

import javax.annotation.Nullable;
import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Storage location configuration.
 *
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_storage_location_conf",
       uniqueConstraints = { @UniqueConstraint(name = "uk_storage_loc_name", columnNames = { "name" }),
                             @UniqueConstraint(name = "uk_storage_loc_conf_type_priority",
                                               columnNames = { StorageLocationConfiguration.STORAGE_TYPE_COLUMN_NAME,
                                                               StorageLocationConfiguration.PRIORITY_COLUMN_NAME }) })
public class StorageLocationConfiguration implements Comparable<StorageLocationConfiguration> {

    public static final String STORAGE_TYPE_COLUMN_NAME = "storage_type";

    public static final String PRIORITY_COLUMN_NAME = "priority";

    public static final long HIGHEST_PRIORITY = 0L;

    @Id
    @SequenceGenerator(name = "storageLocationConfSequence",
                       initialValue = 1,
                       sequenceName = "seq_storage_location_conf")
    @GeneratedValue(generator = "storageLocationConfSequence", strategy = GenerationType.SEQUENCE)
    @ConfigIgnore
    private Long id;

    @Column(length = 128)
    @NotNull
    private String name;

    @OneToOne(optional = true)
    @JoinColumn(nullable = true,
                name = "plugin_conf_id",
                foreignKey = @ForeignKey(name = "fk_prioritized_storage_plugin_conf"))
    private PluginConfiguration pluginConfiguration;

    @Enumerated(EnumType.STRING)
    @Column(name = STORAGE_TYPE_COLUMN_NAME)
    private StorageType storageType = StorageType.OFFLINE;

    @Min(HIGHEST_PRIORITY)
    @Column(name = PRIORITY_COLUMN_NAME)
    private Long priority;

    @Column(name = "allocated_size_ko")
    private Long allocatedSizeInKo = 0L;

    @SuppressWarnings("unused")
    protected StorageLocationConfiguration() {
    }

    public StorageLocationConfiguration(String name, @Nullable PluginConfiguration pluginConf, Long allocatedSizeInKo) {
        this.name = name;
        this.pluginConfiguration = pluginConf;
        this.allocatedSizeInKo = allocatedSizeInKo;
        if (pluginConf != null && pluginConf.getMetaData() != null) {
            if (pluginConf.getInterfaceNames().contains(IOnlineStorageLocation.class.getName())) {
                storageType = StorageType.ONLINE;
            } else if (pluginConf.getInterfaceNames().contains(INearlineStorageLocation.class.getName())) {
                storageType = StorageType.NEARLINE;
            } else {
                storageType = StorageType.OFFLINE;
            }
        }
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

    public String getName() {
        return name;
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

        return pluginConfiguration != null ?
            pluginConfiguration.equals(that.pluginConfiguration) :
            that.pluginConfiguration == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, pluginConfiguration, storageType, priority, allocatedSizeInKo);
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

    public StorageLocationConfigurationDto toDto() {
        return new StorageLocationConfigurationDto(name,
                                                   pluginConfiguration != null ? pluginConfiguration.toDto() : null,
                                                   storageType,
                                                   priority,
                                                   allocatedSizeInKo);
    }

    public static StorageLocationConfiguration fromDto(StorageLocationConfigurationDto configuration) {
        StorageLocationConfiguration storageLocationConfiguration = new StorageLocationConfiguration(configuration.getName(),
                                                                                                     configuration.getPluginConfiguration()
                                                                                                     != null ?
                                                                                                         PluginConfiguration.fromDto(
                                                                                                             configuration.getPluginConfiguration()) :
                                                                                                         null,
                                                                                                     configuration.getAllocatedSizeInKo());
        storageLocationConfiguration.setStorageType(configuration.getStorageType());
        storageLocationConfiguration.setPriority(configuration.getPriority());
        return storageLocationConfiguration;
    }

}
