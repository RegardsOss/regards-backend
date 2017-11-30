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
package fr.cnes.regards.modules.acquisition.domain;

import java.io.File;
import java.security.MessageDigest;
import java.time.OffsetDateTime;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanPlugin;

/**
 * This class represents a {@link MetaFile} instance.<br>
 * A data file is detected by a {@link Plugin} {@link IAcquisitionScanPlugin} 
 * 
 * @author Christophe Mertz
 *
 */
@Entity
@Table(name = "t_acquisition_file")
public class AcquisitionFile implements IIdentifiable<Long>, Cloneable {

    /**
     * Maximum String size constraint with length 255
     */
    private static final int MAX_STRING_NAME_LENGTH = 255;

    /**
     * Maximum enum size constraint with length 16
     */
    private static final int MAX_ENUM_LENGTH = 16;

    /**
     * Unique id
     */
    @Id
    @SequenceGenerator(name = "ChainSequence", initialValue = 1, sequenceName = "seq_chain")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ChainSequence")
    private Long id;

    /**
     * The data file name
     */
    @NotBlank
    @Column(name = "label", length = MAX_STRING_NAME_LENGTH, nullable = false)
    private String fileName;

    /**
     * The data file's size in octets
     */
    @Column(name = "file_size")
    private Long size;

    /**
     * The data file's status
     */
    @Column(name = "status", length = MAX_ENUM_LENGTH)
    @Enumerated(EnumType.STRING)
    private AcquisitionFileStatus status;

    /**
     * The {@link Product} associated to the data file
     */
    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "product_id", referencedColumnName = "ID", foreignKey = @ForeignKey(name = "fk_acq_file_id"),
            updatable = false)
    private Product product;

    /**
     * The {@link MetaFile}
     */
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "meta_file_id", foreignKey = @ForeignKey(name = "fk_meta_file_id"), nullable = true,
            updatable = false)
    private MetaFile metaFile;

    /**
     * File information used to locate the data file
     */
    @Embedded
    private FileAcquisitionInformations acquisitionInformations;

    /**
     * Acquisition date of the data file
     */
    @Column(name = "acquisition_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime acqDate;

    /**
     * Data file checksum
     */
    @Column(name = "checksum", length = MAX_STRING_NAME_LENGTH)
    private String checksum;

    /**
     * Algorithm used to calculate the checksum
     * see {@link MessageDigest}
     */
    @Column(name = "checksumAlgorithm", length = 16)
    private String checksumAlgorithm;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((checksum == null) ? 0 : checksum.hashCode()); //NOSONAR
        result = prime * result + ((fileName == null) ? 0 : fileName.hashCode()); //NOSONAR
        return result;
    }

    @Override
    public boolean equals(Object obj) { // NOSONAR
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AcquisitionFile other = (AcquisitionFile) obj;
        if (checksum == null) {
            if (other.checksum != null) {
                return false;
            }
        } else if (!checksum.equals(other.checksum)) {
            return false;
        }
        if (fileName == null) {
            if (other.fileName != null) {
                return false;
            }
        } else if (!fileName.equals(other.fileName)) {
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public AcquisitionFileStatus getStatus() {
        return status;
    }

    public void setStatus(AcquisitionFileStatus status) {
        this.status = status;
    }

    public MetaFile getMetaFile() {
        return metaFile;
    }

    public void setMetaFile(MetaFile metaFile) {
        this.metaFile = metaFile;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public OffsetDateTime getAcqDate() {
        return acqDate;
    }

    public void setAcqDate(OffsetDateTime acqDate) {
        this.acqDate = acqDate;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String check) {
        this.checksum = check;
    }

    public String getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    public void setChecksumAlgorithm(String checksumAlgorithm) {
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public FileAcquisitionInformations getAcquisitionInformations() {
        return acquisitionInformations;
    }

    public void setAcquisitionInformations(FileAcquisitionInformations acquisitionInformations) {
        this.acquisitionInformations = acquisitionInformations;
    }

    public File getFile() {
        File currentFile = null;
        if (this.getAcquisitionInformations() == null) {
            currentFile = new File(this.getFileName());
        } else {
            String workingDir = this.getAcquisitionInformations().getWorkingDirectory();
            if (workingDir != null) {
                currentFile = new File(workingDir, this.getFileName());
            } else {
                currentFile = new File(this.getAcquisitionInformations().getAcquisitionDirectory(), this.getFileName());
            }
        }
        return currentFile;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        if (id != null) {
            strBuilder.append(id);
            strBuilder.append(" - ");
        }
        strBuilder.append(fileName);
        if (size != null) {
            strBuilder.append(" - ");
            strBuilder.append(size);
        }
        if (status != null) {
            strBuilder.append(" - [");
            strBuilder.append(status);
            strBuilder.append("]");
        }
        return strBuilder.toString();
    }

}
