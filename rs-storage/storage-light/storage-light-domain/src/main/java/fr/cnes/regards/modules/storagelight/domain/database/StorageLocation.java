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
package fr.cnes.regards.modules.storagelight.domain.database;

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
 * @author sbinda
 *
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

    @Column(name = "total_size")
    private Long totalSizeOfReferencedFiles = 0L;

    @Column(name = "allowed_size")
    private Long allowedSize = 0L;

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

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the numberOfReferencedFiles
     */
    public Long getNumberOfReferencedFiles() {
        return numberOfReferencedFiles;
    }

    /**
     * @return the totalSizeOfReferencedFiles
     */
    public Long getTotalSizeOfReferencedFiles() {
        return totalSizeOfReferencedFiles;
    }

    /**
     * @return the allowedSize
     */
    public Long getAllowedSize() {
        return allowedSize;
    }

    /**
     * @return the lastUpdateDate
     */
    public OffsetDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    /**
     * @param lastUpdateDate the lastUpdateDate to set
     */
    public void setLastUpdateDate(OffsetDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    /**
     * @param totalSizeOfReferencedFiles the totalSizeOfReferencedFiles to set
     */
    public void setTotalSizeOfReferencedFiles(Long totalSizeOfReferencedFiles) {
        this.totalSizeOfReferencedFiles = totalSizeOfReferencedFiles;
    }

    /**
     * @param allowedSize the allowedSize to set
     */
    public void setAllowedSize(Long allowedSize) {
        this.allowedSize = allowedSize;
    }

    /**
     * @param numberOfReferencedFiles the numberOfReferencedFiles to set
     */
    public void setNumberOfReferencedFiles(Long numberOfReferencedFiles) {
        this.numberOfReferencedFiles = numberOfReferencedFiles;
    }

}
