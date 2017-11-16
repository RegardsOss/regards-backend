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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;

/**
 * This class represents a data file feature
 * 
 * @author Christophe Mertz
 *
 */
@Entity
@Table(name = "t_dpv_meta_file")
public class MetaFile implements IIdentifiable<Long> {

    /**
     * Maximum file size name constraint with length 255
     */
    private static final int MAX_FILE_NAME_LENGTH = 255;

    @Id
    @SequenceGenerator(name = "MetaFileSequence", initialValue = 1, sequenceName = "seq_meta_file")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MetaFileSequence")
    protected Long id;

    /**
     * <code>true</code> if the data file is mandatory, <code>false</code> otherwise
     */
    @NotNull
    @Column(name = "mandatory")
    private Boolean mandatory = Boolean.FALSE;

    /**
     * Represents the data file name pattern 
     */
    @NotBlank
    @Column(name = "pattern")
    private String fileNamePattern;

    /**
     * A {@link Set} of {@link ScanDirectory} to scan and search data files corresponding to the file name pattern
     */
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "scan_directory_id", referencedColumnName = "ID",
            foreignKey = @ForeignKey(name = "fk_acq_directory"))
    private final Set<ScanDirectory> scanDirectories = new HashSet<ScanDirectory>();

    /**
     * A folder used to move invalid data file
     */
    @Column(name = "invalid_folder_name", length = MAX_FILE_NAME_LENGTH)
    private String invalidFolder;

    /**
     * A {@link String} corresponding to the data file mime-type
     */
    @Column(name = "file_type", length = 16)
    private String fileType;

    /**
     * A comment 
     */
    @Column(name = "comment")
    @Type(type = "text")
    private String comment;

    /**
     * Default constructor
     */
    public MetaFile() {
        super();
    }

    /**
     * Get a {@link ScanDirectory}
     * @param scanDirId the {@link ScanDirectory} identifier 
     * @return a {@link ScanDirectory}
     */
    public ScanDirectory getScanDirectory(Long scanDirId) {
        ScanDirectory supplyDir = null;
        if (scanDirId != null) {
            for (ScanDirectory element : scanDirectories) {
                if (scanDirId.equals(element.getId())) {
                    supplyDir = element;
                    break;
                }
            }
        }
        return supplyDir;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean isMandatory() {
        return mandatory;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public void setFileNamePattern(String fileNamePattern) {
        this.fileNamePattern = fileNamePattern;
    }

    public Set<ScanDirectory> getScanDirectories() {
        return scanDirectories;
    }

    public void addScanDirectory(ScanDirectory scanDirectory) {
        this.scanDirectories.add(scanDirectory);
    }

    public void removeScanDirectory(ScanDirectory scanDirectory) {
        this.scanDirectories.remove(scanDirectory);
    }

    public String getInvalidFolder() {
        return invalidFolder;
    }

    public void setInvalidFolder(String invalidFolder) {
        this.invalidFolder = invalidFolder;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fileNamePattern == null) ? 0 : fileNamePattern.hashCode()); // NOSONAR
        result = prime * result + ((invalidFolder == null) ? 0 : invalidFolder.hashCode()); // NOSONAR
        result = prime * result + ((mandatory == null) ? 0 : mandatory.hashCode()); // NOSONAR
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
        MetaFile other = (MetaFile) obj;
        if (fileNamePattern == null) {
            if (other.fileNamePattern != null) {
                return false;
            }
        } else if (!fileNamePattern.equals(other.fileNamePattern)) {
            return false;
        }
        if (invalidFolder == null) {
            if (other.invalidFolder != null) {
                return false;
            }
        } else if (!invalidFolder.equals(other.invalidFolder)) {
            return false;
        }
        if (mandatory == null) {
            if (other.mandatory != null) {
                return false;
            }
        } else if (!mandatory.equals(other.mandatory)) {
            return false;
        }
        return true;
    }

    public String toString() {
        return id + " - " + fileNamePattern + " - " + mandatory.toString();
    }

}