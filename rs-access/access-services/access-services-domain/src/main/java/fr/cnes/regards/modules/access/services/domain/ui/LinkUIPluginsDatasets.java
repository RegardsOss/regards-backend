/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.access.services.domain.ui;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;

/**
 * Class to link a dataset to a UIPluginConfigurations
 *
 * @author Sébastien Binda
 *
 */
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
@Entity
@Table(name = "t_link_uiservice_dataset",
        uniqueConstraints = @UniqueConstraint(name = "uk_link_uiservice_dataset_dataset_id",
                columnNames = "dataset_id"))
public class LinkUIPluginsDatasets {

    /**
     * Id of the dataset which is concerned by this mapping
     */
    @Id
    @SequenceGenerator(name = "ihmLinkUiPluginDatasetSequence", initialValue = 1,
            sequenceName = "seq_ihm_uiplugin_dataset")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ihmLinkUiPluginDatasetSequence")
    private Long linkId;

    @Column(name = "dataset_id", length = 256)
    private String datasetId;

    /**
     * Ids of plugin configuration of type IService
     *
     * FetchType.EAGER : It is the only usefull information of this POJO. There is no need of getting LinkUIPluginsDatasets without services.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "ta_link_dataset_uiservices",
            joinColumns = @JoinColumn(name = "dataset_id",
                    foreignKey = @ForeignKey(name = "fk_link_dataset_uiservices_dataset_id_service_configuration_id")),
            inverseJoinColumns = @JoinColumn(name = "service_configuration_id",
                    foreignKey = @ForeignKey(name = "fk_link_dataset_uiservices_service_configuration_id_dataset_id")))
    private List<UIPluginConfiguration> services;

    /**
     * Only there for (de)serialization purpose and hibernate
     */
    public LinkUIPluginsDatasets() {
    }

    /**
     * Constructor
     *
     * @param datasetId
     *            Id of the dataset which is concerned by this mapping
     * @param services
     *            Ids of plugin configuration of type IService
     */
    public LinkUIPluginsDatasets(final String datasetId, final List<UIPluginConfiguration> services) {
        super();
        this.datasetId = datasetId;
        this.services = services;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(final String datasetId) {
        this.datasetId = datasetId;
    }

    public List<UIPluginConfiguration> getServices() {
        return services;
    }

    public void setServices(final List<UIPluginConfiguration> services) {
        this.services = services;
    }

}
