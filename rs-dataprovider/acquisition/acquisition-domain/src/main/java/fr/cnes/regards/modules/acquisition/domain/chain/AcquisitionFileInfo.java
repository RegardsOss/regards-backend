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

import javax.persistence.Column;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.oais.urn.DataType;

/**
 *
 * TODO
 * @author Marc Sordi
 *
 */
public class AcquisitionFileInfo {

    @Id
    @SequenceGenerator(name = "MetaFileSequence", initialValue = 1, sequenceName = "seq_meta_file")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "MetaFileSequence")
    protected Long id;

    /**
     * <code>true</code> if the data file is mandatory, <code>false</code> otherwise
     */
    @NotNull
    @Column(name = "mandatory")
    private final Boolean mandatory = Boolean.FALSE;

    @NotNull(message = "Scan plugin is required")
    @ManyToOne(optional = false)
    @JoinColumn(name = "scan_conf_id", nullable = false, foreignKey = @ForeignKey(name = "fk_generation_conf_id"))
    private PluginConfiguration scanPlugin;

    /**
     * A folder used to move invalid data file
     */
    @Column(name = "invalid_folder_name", length = 255)
    private String invalidFolder;

    /**
     * A {@link String} corresponding to the data file mime-type
     */
    @NotNull(message = "Mime type is required")
    @Column(name = "mime_type", length = 255)
    private String mimeType;

    // TODO
    private DataType dataType;

    /**
     * A comment
     */
    @Column(name = "comment")
    @Type(type = "text")
    private String comment;
}
