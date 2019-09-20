/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.domain.dto;

import fr.cnes.regards.modules.storagelight.domain.database.PrioritizedStorage;

/**
 * @author sbinda
 *
 */
public class StorageLocationDTO {

    private String id;

    private StorageLocationType type;

    private Long nbFilesStored;

    private Long totalStoredFilesSizeKo;

    private Long sizeLimitKo;

    private Long nbStorageError;

    private Long nbDeletionError;

    private PrioritizedStorage configuration;

    public static StorageLocationDTO build(String id, StorageLocationType type, Long nbFilesStored,
            Long totalStoredFilesSizeKo, Long sizeLimitKo, Long nbStorageError, Long nbDeletionError,
            PrioritizedStorage configuration) {
        StorageLocationDTO dto = new StorageLocationDTO();
        dto.id = id;
        dto.type = type;
        dto.nbFilesStored = nbFilesStored;
        dto.totalStoredFilesSizeKo = totalStoredFilesSizeKo;
        dto.sizeLimitKo = sizeLimitKo;
        dto.nbStorageError = nbStorageError;
        dto.nbDeletionError = nbDeletionError;
        dto.configuration = configuration;
        return dto;
    }

    public String getId() {
        return id;
    }

    public StorageLocationType getType() {
        return type;
    }

    public Long getNbFilesStored() {
        return nbFilesStored;
    }

    public Long getTotalStoredFilesSizeKo() {
        return totalStoredFilesSizeKo;
    }

    public Long getSizeLimitKo() {
        return sizeLimitKo;
    }

    public Long getNbStorageError() {
        return nbStorageError;
    }

    public Long getNbDeletionError() {
        return nbDeletionError;
    }

    public PrioritizedStorage getConfiguration() {
        return configuration;
    }

}
