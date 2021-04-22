/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;

/**
 * Database definition of the table containing monitoring information about all storage location known.
 *
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_storage_location", indexes = { @Index(name = "idx_storage_location", columnList = "name") },
        uniqueConstraints = { @UniqueConstraint(name = "uk_t_storage_location_name", columnNames = { "name" }) })
public class StorageLocation {

    /**
     * Internal database unique identifier
     */
    @Id
    @SequenceGenerator(name = "storageLocationSequence", initialValue = 1, sequenceName = "seq_storage_location")
    @GeneratedValue(generator = "storageLocationSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(length = 128)
    private String name;

    @Column(name = "nb_ref_files")
    private Long numberOfReferencedFiles = 0L;

    @Column(name = "total_size_ko")
    private Long totalSizeOfReferencedFiles = 0L;

    @Column(name = "last_update_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastUpdateDate;

    public StorageLocation(String name) {
        super();
        this.name = name;
    }

    public StorageLocation() {
        super();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getNumberOfReferencedFiles() {
        return numberOfReferencedFiles;
    }

    public Long getTotalSizeOfReferencedFilesInKo() {
        return totalSizeOfReferencedFiles;
    }

    public OffsetDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(OffsetDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public void setTotalSizeOfReferencedFilesInKo(Long totalSizeOfReferencedFiles) {
        this.totalSizeOfReferencedFiles = totalSizeOfReferencedFiles;
    }

    public void setNumberOfReferencedFiles(Long numberOfReferencedFiles) {
        this.numberOfReferencedFiles = numberOfReferencedFiles;
    }

}
