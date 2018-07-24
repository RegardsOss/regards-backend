/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.nio.file.Path;
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
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.converters.PathAttributeConverter;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;

/**
 * This class represents an acquisition file.<br>
 * This file is created when detected by a scan plugin.
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 *
 */
@Entity
@Table(name = "t_acquisition_file", indexes = { @Index(name = "idx_acq_file_state", columnList = "state"),
        @Index(name = "idx_acq_file_info", columnList = "acq_file_info_id") })
public class AcquisitionFile {

    @Id
    @SequenceGenerator(name = "AcqFileSequence", initialValue = 1, sequenceName = "seq_acq_file")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AcqFileSequence")
    private Long id;

    @NotNull(message = "File path is required")
    @Convert(converter = PathAttributeConverter.class)
    private Path filePath;

    /**
     * The data file's status
     */
    @Column(name = "state", length = 32)
    @Enumerated(EnumType.STRING)
    private AcquisitionFileState state;

    /**
     * This field is only used when acquisition file status is set to {@link AcquisitionFileState#ERROR}
     */
    @Type(type = "text")
    private String error;

    /**
     * The {@link Product} associated to the data file
     */
    @GsonIgnore
    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_acq_file_id"), updatable = false)
    private Product product;

    /**
     * Acquisition date of the data file
     */
    @NotNull(message = "Acquisition date is required")
    @Column(name = "acquisition_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime acqDate;

    /**
     * Data file checksum
     */
    @NotBlank(message = "Checksum is required")
    @Column(name = "checksum", length = 255)
    private String checksum;

    /**
     * Algorithm used to calculate the checksum
     * see {@link MessageDigest}
     */
    @NotBlank(message = "Checksum algorithm is required")
    @Column(name = "checksumAlgorithm", length = 16)
    private String checksumAlgorithm;

    @GsonIgnore
    @NotNull(message = "Acquisition file information is required")
    @ManyToOne
    @JoinColumn(name = "acq_file_info_id", foreignKey = @ForeignKey(name = "fk_acq_file_info_id"), updatable = false)
    private AcquisitionFileInfo fileInfo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    public void setChecksumAlgorithm(String checksumAlgorithm) {
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public AcquisitionFileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(AcquisitionFileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Path getFilePath() {
        return filePath;
    }

    public void setFilePath(Path filePath) {
        this.filePath = filePath;
    }
}
