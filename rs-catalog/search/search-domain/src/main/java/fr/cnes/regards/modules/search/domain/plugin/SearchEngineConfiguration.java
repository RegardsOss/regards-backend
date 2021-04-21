/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.domain.plugin;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import fr.cnes.regards.framework.module.manager.ConfigIgnore;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;

/**
 * Configuration POJO for {@link ISearchEngine} plugins.
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_search_engine_conf",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "plugin_conf_id", "dataset_urn" }) })
public class SearchEngineConfiguration {

    @Id
    @SequenceGenerator(name = "searchEnginConfSequence", initialValue = 1, sequenceName = "seq_search_engine_conf")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "searchEnginConfSequence")
    @ConfigIgnore
    private Long id;

    @NotNull
    @Column(name = "label", nullable = false, length = 256)
    private String label;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "plugin_conf_id", referencedColumnName = "id")
    private PluginConfiguration configuration;

    @Column(name = "dataset_urn", nullable = true, length = 256)
    private String datasetUrn;

    @Transient
    @ConfigIgnore
    private Dataset dataset;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PluginConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(PluginConfiguration configuration) {
        this.configuration = configuration;
    }

    public String getDatasetUrn() {
        return datasetUrn;
    }

    public void setDatasetUrn(String datasetUrn) {
        this.datasetUrn = datasetUrn;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}
