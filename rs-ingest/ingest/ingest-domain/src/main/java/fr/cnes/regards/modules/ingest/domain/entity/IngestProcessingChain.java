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
package fr.cnes.regards.modules.ingest.domain.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;

/**
 * System POJO for storing configured processing chain
 * @author Marc Sordi
 *
 */
@Entity
@Table(name = "t_ingest_processing_chain",
        uniqueConstraints = { @UniqueConstraint(name = "uk_ingest_chain_name", columnNames = "name") })
public class IngestProcessingChain {

    @Id
    @SequenceGenerator(name = "IngestChainSequence", initialValue = 1, sequenceName = "seq_ingest_chain")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "IngestChainSequence")
    private Long id;

    /**
     * Name of the processing chain
     */
    @NotBlank
    @Size(min = 3, max = 50, message = "Processing chain name must be between 3 and 50 characters long")
    @Pattern(regexp = "[0-9a-zA-Z_]*",
            message = "Processing chain name must only contain alphanumerical characters or underscore.")
    @Column(length = 50, nullable = false, updatable = false)
    private String name;

    /**
     * Optional chain description
     */
    @Column(length = 128)
    private String description;

    @ManyToOne
    @JoinColumn(name = "preprocessing_conf_id", foreignKey = @ForeignKey(name = "fk_preprocessing_conf_id"))
    private PluginConfiguration preProcessingPlugin;

    @ManyToOne(optional = false)
    @JoinColumn(name = "validation_conf_id", nullable = false, foreignKey = @ForeignKey(name = "fk_validation_conf_id"))
    private PluginConfiguration validationPlugin;

    @ManyToOne(optional = false)
    @JoinColumn(name = "generation_conf_id", nullable = false, foreignKey = @ForeignKey(name = "fk_generation_conf_id"))
    private PluginConfiguration generationPlugin;

    @ManyToOne
    @JoinColumn(name = "tag_conf_id", foreignKey = @ForeignKey(name = "fk_tag_conf_id"))
    private PluginConfiguration tagPlugin;

    @ManyToOne
    @JoinColumn(name = "postprocessing_conf_id", foreignKey = @ForeignKey(name = "fk_postprocessing_conf_id"))
    private PluginConfiguration postProcessingPlugin;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PluginConfiguration getPreProcessingPlugin() {
        return preProcessingPlugin;
    }

    public void setPreProcessingPlugin(PluginConfiguration preProcessingPlugin) {
        this.preProcessingPlugin = preProcessingPlugin;
    }

    public PluginConfiguration getValidationPlugin() {
        return validationPlugin;
    }

    public void setValidationPlugin(PluginConfiguration validationPlugin) {
        this.validationPlugin = validationPlugin;
    }

    public PluginConfiguration getGenerationPlugin() {
        return generationPlugin;
    }

    public void setGenerationPlugin(PluginConfiguration generationPlugin) {
        this.generationPlugin = generationPlugin;
    }

    public PluginConfiguration getTagPlugin() {
        return tagPlugin;
    }

    public void setTagPlugin(PluginConfiguration tagPlugin) {
        this.tagPlugin = tagPlugin;
    }

    public PluginConfiguration getPostProcessingPlugin() {
        return postProcessingPlugin;
    }

    public void setPostProcessingPlugin(PluginConfiguration postProcessingPlugin) {
        this.postProcessingPlugin = postProcessingPlugin;
    }
}
