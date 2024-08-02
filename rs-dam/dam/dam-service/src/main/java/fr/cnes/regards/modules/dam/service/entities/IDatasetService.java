/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.dam.service.entities;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.Errors;

import java.util.Set;

/**
 * Qualified interface for Dataset entity service
 *
 * @author oroussel
 */
public interface IDatasetService extends IEntityService<Dataset> {

    /**
     * Create a dataset
     */
    Dataset createDataset(Dataset dataset, Errors errors) throws ModuleException;

    /**
     * Update dataset
     *
     * @param datasetId internal identifier
     * @param dataset   the dataset to update
     * @param errors    validation errors
     * @return updated dataset
     */
    Dataset updateDataset(Long datasetId, Dataset dataset, Errors errors) throws ModuleException;

    /**
     * Extract the AttributeModel of {@link DataObject} that can be contained into datasets. <br/>
     * If pUrns is null or empty AND pModelName is null then the scope of datasets is not restrained.<br/>
     * If pUrns is not null and not empty AND pModelName is not null then we only consider pModelName.<br/>
     * If pModelName is not null then the scope of datasets is restrained to all datasets complient with the given
     * model.<br/>
     * If pUrns is not null and not empty AND pModelName is null then the scope of datasets is restrained to all the
     * datasets represented by the given urns
     *
     * @param pUrns {@link UniformResourceName}s
     * @return {@link AttributeModel}s
     */
    Page<AttributeModel> getDataAttributeModels(Set<UniformResourceName> pUrns,
                                                Set<String> modelNames,
                                                Pageable pPageable) throws ModuleException;

    /**
     * Retrieve {@link AttributeModel}s associated to the given {@link Dataset}s or {@link Model}s given.
     *
     * @return {@link AttributeModel}s
     */
    Page<AttributeModel> getAttributeModels(Set<UniformResourceName> pUrns, Set<String> modelNames, Pageable pPageable)
        throws ModuleException;

    /**
     * Validate clause
     *
     * @param clause to be validate
     * @return true if clause is valid, false otherwise
     */
    boolean validateOpenSearchSubsettingClause(String clause);

    /**
     * Get the number of datasets associated to a given datasource plugin configuration.
     *
     * @param dataSourcePluginConfId identifier of the {@link PluginConfiguration} of the datasource
     * @return number of datasets.
     */
    Long countByDataSource(Long dataSourcePluginConfId);

    Set<Dataset> findAllByModel(Long modelId);
}