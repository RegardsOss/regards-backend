/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.fileaccess.dto;

import fr.cnes.regards.framework.modules.plugins.dto.PluginConfigurationDto;
import org.springframework.lang.Nullable;

/**
 * Dto for a Storage Location Configuration
 *
 * @author Thibaud Michaudel
 **/
public class StorageLocationConfigurationDto {

    private Long id;

    private String name;

    private PluginConfigurationDto pluginConfiguration;

    private StorageType storageType = StorageType.OFFLINE;

    private Long priority;

    private Long allocatedSizeInKo = 0L;

    public StorageLocationConfigurationDto(Long id,
                                           String name,
                                           @Nullable PluginConfigurationDto pluginConfiguration,
                                           StorageType storageType,
                                           Long priority,
                                           Long allocatedSizeInKo) {
        this.id = id;
        this.name = name;
        this.pluginConfiguration = pluginConfiguration;
        this.storageType = storageType;
        this.priority = priority;
        this.allocatedSizeInKo = allocatedSizeInKo;
    }

    public StorageLocationConfigurationDto(String name,
                                           @Nullable PluginConfigurationDto pluginConfiguration,
                                           Long priority,
                                           Long allocatedSizeInKo) {
        this.name = name;
        this.pluginConfiguration = pluginConfiguration;
        this.priority = priority;
        this.allocatedSizeInKo = allocatedSizeInKo;
    }

    public StorageLocationConfigurationDto(String name,
                                           @Nullable PluginConfigurationDto pluginConfiguration,
                                           StorageType storageType,
                                           Long priority) {
        this.name = name;
        this.pluginConfiguration = pluginConfiguration;
        this.storageType = storageType;
        this.priority = priority;
    }

    public StorageLocationConfigurationDto(String name,
                                           @Nullable PluginConfigurationDto pluginConfiguration,
                                           Long priority) {
        this.name = name;
        this.pluginConfiguration = pluginConfiguration;
        this.priority = priority;
    }

    public String getName() {
        return name;
    }

    public PluginConfigurationDto getPluginConfiguration() {
        return pluginConfiguration;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public Long getPriority() {
        return priority;
    }

    public Long getAllocatedSizeInKo() {
        return allocatedSizeInKo;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPluginConfiguration(PluginConfigurationDto pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    public void setPriority(Long priority) {
        this.priority = priority;
    }

    public void setAllocatedSizeInKo(Long allocatedSizeInKo) {
        this.allocatedSizeInKo = allocatedSizeInKo;
    }
}
