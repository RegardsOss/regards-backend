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
package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.regards;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.rometools.rome.feed.module.Module;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.attribute.AbstractAttribute;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.AttributeCriterionBuilder;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchParameterConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.ParameterOperator;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.description.DescriptionParameter;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.exception.UnsupportedCriterionOperator;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.AbstractOpenSearchExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.SearchParameter;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.formatter.atom.modules.regards.RegardsModule;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.formatter.atom.modules.regards.impl.RegardsModuleImpl;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.schema.parameters.OpenSearchParameter;

/**
 * Regards parameter extension for Opensearch standard.
 * Add regards namespace to all parameters from the specific projects models attributes.
 * This extension handles all parameters not configured, or configured with the REGARDS namespace.
 *
 * @see <a href="https://github.com/dewitt/opensearch/blob/master/opensearch-1-1-draft-6.md">Opensearch</a>
 * @see <a href="http://www.opensearch.org/Specifications/OpenSearch/Extensions/Parameter/1.0/Draft_2">Opensearch parameter extension</a>
 *
 * @author Sébastien Binda
 */
public class RegardsExtension extends AbstractOpenSearchExtension {

    /**
     * REGARDS namespace for parameters of current extension.
     */
    public static final String REGARDS_NS = "regards";

    @Override
    public void formatGeoJsonResponseFeature(AbstractEntity entity,
            List<OpenSearchParameterConfiguration> paramConfigurations, Feature feature) {
        Map<String, Object> properties = Maps.newHashMap();
        for (AbstractAttribute<?> property : entity.getProperties()) {
            properties.put(property.getName(), property.getValue());
        }
        feature.setProperties(properties);
    }

    @Override
    public Module getAtomEntityResponseBuilder(AbstractEntity entity,
            List<OpenSearchParameterConfiguration> paramConfigurations, Gson gson) {
        RegardsModule rm = new RegardsModuleImpl();
        rm.setGsonBuilder(gson);
        rm.setEntity(entity);
        return rm;
    }

    @Override
    public void applyToDescriptionParameter(OpenSearchParameter parameter, DescriptionParameter descParameter) {
        // Nothing to do
    }

    @Override
    public void applyToDescription(OpenSearchDescription openSearchDescription) {
        // Nothing to do
    }

    @Override
    protected ICriterion buildCriteria(SearchParameter parameter) throws UnsupportedCriterionOperator {
        return AttributeCriterionBuilder.build(parameter.getAttributeModel(), ParameterOperator.EQ,
                                               parameter.getSearchValues());
    }

    @Override
    protected boolean supportsSearchParameter(OpenSearchParameterConfiguration conf) {
        return (conf == null) || (conf.getNamespace() == null) || REGARDS_NS.equals(conf.getNamespace());
    }

}
