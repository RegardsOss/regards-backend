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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.rometools.rome.feed.module.Module;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.attribute.AbstractAttribute;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.opensearch.service.cache.attributemodel.IAttributeFinder;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.AttributeCriterionBuilder;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchParameterConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.ParameterOperator;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.IOpenSearchExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.formatter.atom.modules.regards.RegardsModule;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.formatter.atom.modules.regards.impl.RegardsModuleImpl;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.schema.OpenSearchParameter;

/**
 * Regards parameter extension for Opensearch standard.
 * Add regards namespace to all parameters from the specific projects models attributes.
 * @see <a href="https://github.com/dewitt/opensearch/blob/master/opensearch-1-1-draft-6.md">Opensearch</a>
 * @see <a href="http://www.opensearch.org/Specifications/OpenSearch/Extensions/Parameter/1.0/Draft_2">Opensearch parameter extension</a>
 *
 * @author Sébastien Binda
 */
public class RegardsExtension implements IOpenSearchExtension {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RegardsExtension.class);

    /**
     * Does the current extension activated ?
     */
    private boolean activated = false;

    @Override
    public boolean isActivated() {
        return activated;
    }

    @Override
    public void applyExtensionToGeoJsonFeature(AbstractEntity entity, Feature feature) {
        Map<String, Object> properties = Maps.newHashMap();
        for (AbstractAttribute<?> property : entity.getProperties()) {
            properties.put(property.getName(), property.getValue());
        }
        feature.setProperties(properties);
    }

    @Override
    public Module getAtomEntityBuilderModule(AbstractEntity entity, Gson gson) {
        RegardsModule rm = new RegardsModuleImpl();
        rm.setGsonBuilder(gson);
        rm.setEntity(entity);
        return rm;
    }

    @Override
    public void applyExtensionToDescriptionParameter(OpenSearchParameter parameter) {
        // Nothing to do
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    @Override
    public void applyExtensionToDescription(OpenSearchDescription openSearchDescription) {
        // Nothing to do
    }

    @Override
    public ICriterion buildCriterion(MultiValueMap<String, String> queryParams,
            List<OpenSearchParameterConfiguration> configurations, IAttributeFinder finder) {
        List<ICriterion> criteria = new ArrayList<>();

        for (Entry<String, List<String>> queryParam : queryParams.entrySet()) {
            // Get couple parameter name/values
            String paramName = queryParam.getKey();
            List<String> values = queryParam.getValue();
            // Find associated attribute configuration from plugin conf
            Optional<OpenSearchParameterConfiguration> oParam = configurations.stream()
                    .filter(p -> p.getName().equals(paramName)).findFirst();
            try {
                if (oParam.isPresent()) {
                    OpenSearchParameterConfiguration conf = oParam.get();
                    // Parse attribute value to create associated ICriterion using parameter configuration
                    criteria.add(AttributeCriterionBuilder.build(conf, values, finder));
                } else {
                    criteria.add(AttributeCriterionBuilder.build(paramName, ParameterOperator.EQ, values, finder));
                }
            } catch (OpenSearchUnknownParameter e) {
                LOGGER.error("Invalid public attribute {}. Unknown type.", paramName);
            }
        }
        return criteria.isEmpty() ? ICriterion.all() : ICriterion.and(criteria);
    }

}
