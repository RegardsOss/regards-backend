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

import java.security.MessageDigest;
import java.time.OffsetDateTime;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;

/**
 * This class represents an acquisition file.<br>
 * This file is created when detected by a scan plugin.
 *
 * @author Christophe Mertz
 *
 */
@Entity
@Table(name = "t_acquisition_file")
public class AcquisitionFile implements IIdentifiable<Long> {

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
    @Column(name = "state", length = MAX_ENUM_LENGTH)
    @Enumerated(EnumType.STRING)
    private AcquisitionFileState state;

    /**
     * The {@link Product} associated to the data file
     */
    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_acq_file_id"), updatable = false)
    private Product product;

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
    public int hashCode() { // NOSONAR
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((checksum == null) ? 0 : checksum.hashCode()); // NOSONAR
        result = (prime * result) + ((fileName == null) ? 0 : fileName.hashCode()); // NOSONAR
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

    public AcquisitionFileState getState() {
        return state;
    }

    public void setState(AcquisitionFileState state) {
        this.state = state;
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
        if (state != null) {
            strBuilder.append(" - [");
            strBuilder.append(state);
            strBuilder.append("]");
        }
        return strBuilder.toString();
    }

}
