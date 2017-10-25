/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.domain.metadata;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;

/**
 * Represents a directory to scan data files
 * 
 * @author Christophe Mertz
 *
 */
@Entity
@Table(name = "t_scan_directory")
public class ScanDirectory implements IIdentifiable<Long> {

    @Id
    @SequenceGenerator(name = "ScanDirSequence", initialValue = 1, sequenceName = "seq_scan_dir")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ScanDirSequence")
    protected Long id;

    /**
     * The directory name
     */
    @NotBlank
    @Column(name = "scan_directory")
    private String scanDir;

//    /**
//     * Acquisition date of the last acquired file in the current directory
//     */
//    @Column(name = "last_acquisition_date")
//    @Convert(converter = OffsetDateTimeAttributeConverter.class)
//    private OffsetDateTime lastAcqDate;

    /**
     * Default constructor
     */
    public ScanDirectory() {
        super();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((scanDir == null) ? 0 : scanDir.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ScanDirectory other = (ScanDirectory) obj;
        if (scanDir == null) {
            if (other.scanDir != null) {
                return false;
            }
        } else if (!scanDir.equals(other.scanDir)) {
            return false;
        }
        return true;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getScanDir() {
        return scanDir;
    }

    public void setScanDir(String scanDir) {
        this.scanDir = scanDir;
    }

//    public OffsetDateTime getLastAcqDate() {
//        return lastAcqDate;
//    }
//
//    public void setLastAcqDate(OffsetDateTime lastAcqDate) {
//        this.lastAcqDate = lastAcqDate;
//    }

}
