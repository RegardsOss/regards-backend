/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.fileaccess.domain;

import fr.cnes.regards.framework.module.manager.ConfigIgnore;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.fileaccess.dto.StorageLocationConfigurationDto;
import fr.cnes.regards.modules.fileaccess.dto.StorageType;
import fr.cnes.regards.modules.fileaccess.plugin.domain.INearlineStorageLocation;
import fr.cnes.regards.modules.fileaccess.plugin.domain.IOnlineStorageLocation;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * Storage location configuration.
 *
 * @author Sébastien Binda
 * @author Thibaud Michaudel
 */
@Entity
@Table(name = "t_storage_location_conf",
       uniqueConstraints = { @UniqueConstraint(name = "uk_storage_loc_name", columnNames = { "name" }) })
public class StorageLocationConfiguration {

    public static final String STORAGE_TYPE_COLUMN_NAME = "storage_type";

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
    @JoinColumn(nullable = true, name = "plugin_conf_id", foreignKey = @ForeignKey(name = "fk_storage_plugin_conf"))
    private PluginConfiguration pluginConfiguration;

    @Enumerated(EnumType.STRING)
    @Column(name = STORAGE_TYPE_COLUMN_NAME)
    private StorageType storageType = StorageType.OFFLINE;

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

    public static StorageLocationConfiguration fromDto(StorageLocationConfigurationDto configuration) {
        StorageLocationConfiguration storageLocationConfiguration = new StorageLocationConfiguration(configuration.getName(),
                                                                                                     configuration.getPluginConfiguration()
                                                                                                     != null ?
                                                                                                         PluginConfiguration.fromDto(
                                                                                                             configuration.getPluginConfiguration()) :
                                                                                                         null,
                                                                                                     configuration.getAllocatedSizeInKo());
        storageLocationConfiguration.setStorageType(configuration.getStorageType());
        return storageLocationConfiguration;
    }

    public PluginConfiguration getPluginConfiguration() {
        return pluginConfiguration;
    }

    public void setPluginConfiguration(PluginConfiguration dataStorageConfiguration) {
        this.pluginConfiguration = dataStorageConfiguration;
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

        return Objects.equals(pluginConfiguration, that.pluginConfiguration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, pluginConfiguration, storageType, allocatedSizeInKo);
    }

    public StorageLocationConfigurationDto toDto() {
        // FIXME: the priority attribute is not migrated in rs-file-access. However, to keep backwards compatibility
        //  (until the migration of rs-storage is completed) the priority is initialized to 0 by default.
        return new StorageLocationConfigurationDto(id,
                                                   name,
                                                   pluginConfiguration != null ? pluginConfiguration.toDto() : null,
                                                   storageType,
                                                   0L,
                                                   allocatedSizeInKo);
    }

}
