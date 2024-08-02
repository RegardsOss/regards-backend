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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension;

import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.ParameterConfiguration;

import java.util.List;

/**
 * Search parameter for an opensearch standard request.
 * {@link AttributeModel} is used to define which attribute the parameter is about
 * {@link ParameterConfiguration} is used to get the opensearch configuration for the attribute
 * {@link String}s are the values to apply to the attribute in {@link ICriterion} to run search.
 *
 * @author Sébastien Binda
 */
public class SearchParameter {

    /**
     * Search parameter nae
     */
    private String name;

    /**
     * Define which attribute the parameter is about
     */
    private AttributeModel attributeModel;

    /**
     * Define opensearch configuration for the attribute
     */
    private ParameterConfiguration configuration;

    /**
     * Values to apply to the attribute in {@link ICriterion} to run search.
     */
    private List<String> searchValues;

    public SearchParameter(String name,
                           AttributeModel attributeModel,
                           ParameterConfiguration configuration,
                           List<String> searchValues) {
        super();
        this.name = name;
        this.attributeModel = attributeModel;
        this.configuration = configuration;
        this.searchValues = searchValues;
    }

    public AttributeModel getAttributeModel() {
        return attributeModel;
    }

    public void setAttributeModel(AttributeModel attributeModel) {
        this.attributeModel = attributeModel;
    }

    public ParameterConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ParameterConfiguration configuration) {
        this.configuration = configuration;
    }

    public List<String> getSearchValues() {
        return searchValues;
    }

    public void setSearchValues(List<String> searchValues) {
        this.searchValues = searchValues;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
