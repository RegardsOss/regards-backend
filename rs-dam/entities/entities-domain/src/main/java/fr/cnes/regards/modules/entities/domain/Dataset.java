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
package fr.cnes.regards.modules.entities.domain;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.Type;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.entities.domain.metadata.DatasetMetadata;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.models.domain.Model;

/**
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 * @author Christophe Mertz
 * @author oroussel
 */
@Entity
@DiscriminatorValue("DATASET")
public class Dataset extends AbstractDescEntity {

    public static final String DATA_SOURCE_ID = "dataSourceId";

    public static final String LAST_UPDATE = "lastUpdate";

    /**
     * value allowing the system to order a set of result
     */
    @Column
    private int score;

    /**
     * A PluginConfiguration for a plugin type IDataSourcePlugin.</br>
     * This PluginConfiguration defined the DataSource from which this Dataset presents data. <b>nullable = true</b> is
     * necessary because of single-table entity mapping (same table is used for all types of entities and other haven't
     * plugin configuration).
     */
    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "ds_plugin_conf_id", foreignKey = @ForeignKey(name = "fk_ds_plugin_conf_id"), nullable = true,
            updatable = false)
    private PluginConfiguration plgConfDataSource;

    /**
     * Model id of the Data objects held by this Dataset. nullable=true because fo single table
     */
    @Column(name = "data_model_id", updatable = false, nullable = true)
    private Long dataModel;

    /**
     * Request clause to subset data from the DataSource, only used by the catalog(elasticsearch) as all data from
     * DataSource has been given to the catalog
     */
    @Type(type = "jsonb")
    @Column(name = "sub_setting_clause", columnDefinition = "jsonb")
    private ICriterion subsettingClause;

    /**
     * Representation of the above subsetting clause as an OpenSearch string request
     */
    @Type(type = "text")
    @Column
    private String openSearchSubsettingClause;

    /**
     * set of quotations associated to the {@link Dataset}
     */
    @ElementCollection
    @CollectionTable(name = "t_dataset_quotation", joinColumns = @JoinColumn(name = "dataset_id"),
            foreignKey = @javax.persistence.ForeignKey(name = "fk_dataset_quotation_dataset_id"))
    private Set<String> quotations = new HashSet<>();

    /**
     * Dataset licence
     */
    @Type(type = "text")
    @Column
    private String licence;

    /**
     * Metadata, only used by Elasticsearch
     */
    @Transient
    private DatasetMetadata metadata = new DatasetMetadata();

    public Dataset() {
        // we use super and not this because at deserialization we need a ipId null at the object creation which is then replaced by the attribute if present or added by creation method
        super(null, null, null);
    }

    public Dataset(Model pModel, String pTenant, String pLabel) {
        super(pModel, new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET, pTenant, UUID.randomUUID(), 1),
              pLabel);
    }

    /**
     * @return the score
     */
    public int getScore() {
        return score;
    }

    /**
     * Set the score
     * @param pScore
     */
    public void setScore(int pScore) {
        score = pScore;
    }

    @Override
    public String getType() {
        return EntityType.DATASET.toString();
    }

    /**
     * Get the ICriterion tree containing DATA_SOURCE_ID restriction ie different from saved subsetting clause and
     * opensearch one which are user-friendly (do not contain views of intern structure)
     */
    public ICriterion getSubsettingClause() {
        ICriterion subsettingCrit = subsettingClause;
        // Add datasource id restriction
        if ((subsettingCrit == null) || (subsettingCrit == ICriterion.all())) {
            subsettingCrit = ICriterion.eq(DATA_SOURCE_ID, plgConfDataSource.getId());
        } else {
            subsettingCrit = ICriterion.and(subsettingCrit, ICriterion.eq(DATA_SOURCE_ID, plgConfDataSource.getId()));
        }
        return subsettingCrit;
    }

    /**
     * Set the subsetting clause
     * @param pSubsettingClause
     */
    public void setSubsettingClause(ICriterion pSubsettingClause) {
        subsettingClause = pSubsettingClause;
    }

    public ICriterion getUserSubsettingClause() {
        return (subsettingClause == null) ? ICriterion.all() : subsettingClause;
    }

    public PluginConfiguration getDataSource() {
        return plgConfDataSource;
    }

    public void setDataSource(PluginConfiguration pPlgConfDataSource) {
        plgConfDataSource = pPlgConfDataSource;
    }

    /**
     * @return the data model
     */
    public Long getDataModel() {
        return dataModel;
    }

    /**
     * Set the data model
     * @param pDataModel
     */
    public void setDataModel(Long pDataModel) {
        dataModel = pDataModel;
    }

    /**
     * @return the quotations
     */
    public Set<String> getQuotations() {
        return quotations;
    }

    /**
     * Set the quotations
     * @param pQuotations
     */
    public void setQuotations(Set<String> pQuotations) {
        quotations = pQuotations;
    }

    /**
     * Add a quotation
     * @param pQuotation
     */
    public void addQuotation(String pQuotation) {
        quotations.add(pQuotation);
    }

    /**
     * @return the licence
     */
    public String getLicence() {
        return licence;
    }

    /**
     * Set the licence
     * @param pLicence
     */
    public void setLicence(String pLicence) {
        licence = pLicence;
    }

    public DatasetMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(DatasetMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object pObj) {
        return super.equals(pObj);
    }

    /**
     * @return the openSearchSubsettingClause
     */
    public String getOpenSearchSubsettingClause() {
        return openSearchSubsettingClause;
    }

    /**
     * @param pOpenSearchSubsettingClause the openSearchSubsettingClause to set
     */
    public void setOpenSearchSubsettingClause(String pOpenSearchSubsettingClause) {
        openSearchSubsettingClause = pOpenSearchSubsettingClause;
    }
}
