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
package fr.cnes.regards.modules.acquisition.domain.chain;

import java.time.OffsetDateTime;

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
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.oais.urn.DataType;

/**
 *
 * Acquisition file information. An acquisition file is an optional or mandatory part of a product.
 *
 * @author Marc Sordi
 */
@Entity
@Table(name = "t_acq_file_info")
public class AcquisitionFileInfo {

    @Id
    @SequenceGenerator(name = "AcqFileInfoSequence", initialValue = 1, sequenceName = "seq_acq_file_info")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "AcqFileInfoSequence")
    protected Long id;

    /**
     * <code>true</code> if the related data file is mandatory, <code>false</code> otherwise
     */
    @NotNull
    @Column(name = "mandatory")
    private final Boolean mandatory = Boolean.FALSE;

    @NotNull(message = "Scan plugin is required")
    @ManyToOne(optional = false)
    @JoinColumn(name = "scan_conf_id", nullable = false, foreignKey = @ForeignKey(name = "fk_scan_conf_id"))
    private PluginConfiguration scanPlugin;

    /**
     * The most recent last modification date of all scanned files as millisecond
     */
    @Column(name = "lastModificationDate")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastModificationDate;

    /**
     * A {@link String} corresponding to the data file mime-type
     */
    @NotBlank(message = "Mime type is required")
    @Column(name = "mime_type", length = 255)
    private String mimeType;

    @NotNull(message = "REGARDS data type is required")
    @Column(name = "data_type", length = 16)
    @Enumerated(EnumType.STRING)
    private DataType dataType;

    /**
     * A comment
     */
    @Column(name = "comment")
    @Type(type = "text")
    private String comment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PluginConfiguration getScanPlugin() {
        return scanPlugin;
    }

    public void setScanPlugin(PluginConfiguration scanPlugin) {
        this.scanPlugin = scanPlugin;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean isMandatory() {
        return mandatory;
    }

    public OffsetDateTime getLastModificationDate() {
        return lastModificationDate;
    }

    public void setLastModificationDate(OffsetDateTime lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }
}
