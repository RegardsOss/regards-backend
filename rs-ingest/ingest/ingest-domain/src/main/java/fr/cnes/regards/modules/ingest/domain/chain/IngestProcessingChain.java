/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.chain;

import fr.cnes.regards.framework.module.manager.ConfigIgnore;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * System POJO for storing configured processing chain
 *
 * @author Marc Sordi
 */
@Entity
@Table(name = "t_ingest_processing_chain",
       uniqueConstraints = { @UniqueConstraint(name = "uk_ingest_chain_name", columnNames = "name") })
@NamedEntityGraph(name = "graph.ingest.processing.chain.complete",
                  attributeNodes = { @NamedAttributeNode(value = "preProcessingPlugin"),
                                     @NamedAttributeNode(value = "validationPlugin"),
                                     @NamedAttributeNode(value = "generationPlugin"),
                                     @NamedAttributeNode(value = "aipStorageMetadataPlugin"),
                                     @NamedAttributeNode(value = "tagPlugin"),
                                     @NamedAttributeNode(value = "postProcessingPlugin") })
public class IngestProcessingChain {

    public static final String DEFAULT_INGEST_CHAIN_LABEL = "DefaultProcessingChain";

    @ConfigIgnore
    @Id
    @SequenceGenerator(name = "IngestChainSequence", initialValue = 1, sequenceName = "seq_ingest_chain")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "IngestChainSequence")
    private Long id;

    /**
     * Name of the processing chain
     */
    @NotBlank(message = "Ingest processing chain name is required")
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

    @NotNull(message = "Validation plugin is required")
    @ManyToOne(optional = false)
    @JoinColumn(name = "validation_conf_id", nullable = false, foreignKey = @ForeignKey(name = "fk_validation_conf_id"))
    private PluginConfiguration validationPlugin;

    @NotNull(message = "Generation plugin is required")
    @ManyToOne(optional = false)
    @JoinColumn(name = "generation_conf_id", nullable = false, foreignKey = @ForeignKey(name = "fk_generation_conf_id"))
    private PluginConfiguration generationPlugin;

    @ManyToOne
    @JoinColumn(name = "aip_storage_metadata_conf_id",
                foreignKey = @ForeignKey(name = "fk_aip_storage_metadata_conf_id"))
    private PluginConfiguration aipStorageMetadataPlugin;

    @ManyToOne
    @JoinColumn(name = "tag_conf_id", foreignKey = @ForeignKey(name = "fk_tag_conf_id"))
    private PluginConfiguration tagPlugin;

    @ManyToOne
    @JoinColumn(name = "postprocessing_conf_id", foreignKey = @ForeignKey(name = "fk_postprocessing_conf_id"))
    private PluginConfiguration postProcessingPlugin;

    public IngestProcessingChain(String name,
                                 String description,
                                 PluginConfiguration validationPlugin,
                                 PluginConfiguration generationPlugin) {
        super();
        this.name = name;
        this.description = description;
        this.validationPlugin = validationPlugin;
        this.generationPlugin = generationPlugin;
    }

    public IngestProcessingChain() {
        super();
    }

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

    public Optional<PluginConfiguration> getPreProcessingPlugin() {
        return Optional.ofNullable(preProcessingPlugin);
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

    public Optional<PluginConfiguration> getTagPlugin() {
        return Optional.ofNullable(tagPlugin);
    }

    public void setTagPlugin(PluginConfiguration tagPlugin) {
        this.tagPlugin = tagPlugin;
    }

    public Optional<PluginConfiguration> getPostProcessingPlugin() {
        return Optional.ofNullable(postProcessingPlugin);
    }

    public void setPostProcessingPlugin(PluginConfiguration postProcessingPlugin) {
        this.postProcessingPlugin = postProcessingPlugin;
    }

    public Optional<PluginConfiguration> getAipStorageMetadataPlugin() {
        return Optional.ofNullable(aipStorageMetadataPlugin);
    }

    public void setAipStorageMetadataPlugin(PluginConfiguration aipStorageMetadataPlugin) {
        this.aipStorageMetadataPlugin = aipStorageMetadataPlugin;
    }

    /**
     * @return list of the really configured plugins for the current chain
     */
    public List<PluginConfiguration> getChainPlugins() {
        List<PluginConfiguration> confs = new ArrayList<>();
        if (preProcessingPlugin != null) {
            confs.add(preProcessingPlugin);
        }
        confs.add(validationPlugin);
        confs.add(generationPlugin);
        if (aipStorageMetadataPlugin != null) {
            confs.add(aipStorageMetadataPlugin);
        }
        if (tagPlugin != null) {
            confs.add(tagPlugin);
        }
        if (postProcessingPlugin != null) {
            confs.add(postProcessingPlugin);
        }
        return confs;
    }
}
