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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.regards;

import com.google.gson.Gson;
import com.rometools.rome.feed.atom.Entry;
import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.dto.properties.ObjectProperty;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.schema.parameters.OpenSearchParameter;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.AttributeCriterionBuilder;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.ParameterConfiguration;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.ParameterOperator;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.description.DescriptionParameter;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.exception.ExtensionException;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.exception.UnsupportedCriterionOperator;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.AbstractExtension;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.SearchParameter;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.regards.RegardsModule;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.regards.impl.RegardsModuleImpl;

import java.util.*;

/**
 * Regards parameter extension for Opensearch standard.
 * Add regards namespace to all parameters from the specific projects models attributes.
 * This extension handles all parameters not configured, or configured with the REGARDS namespace.
 *
 * @author Sébastien Binda
 * @see <a href="https://github.com/dewitt/opensearch/blob/master/opensearch-1-1-draft-6.md">Opensearch</a>
 * @see <a href="http://www.opensearch.org/Specifications/OpenSearch/Extensions/Parameter/1.0/Draft_2">Opensearch parameter extension</a>
 */
public class RegardsExtension extends AbstractExtension {

    /**
     * REGARDS namespace for parameters of current extension.
     */
    public static final String REGARDS_NS = "regards";

    @Override
    public void formatGeoJsonResponseFeature(EntityFeature entity,
                                             List<ParameterConfiguration> paramConfigurations,
                                             Feature feature,
                                             String token) {
        feature.addProperty("tags", entity.getTags());
        // Report existing one
        feature.getProperties().putAll(buildProperties(entity.getProperties()));
    }

    private Map<String, Object> buildProperties(Set<IProperty<?>> properties) {
        Map<String, Object> nested = new HashMap<>();
        if (properties != null) {
            for (IProperty<?> property : properties) {
                if (property.represents(PropertyType.OBJECT)) {
                    ObjectProperty op = (ObjectProperty) property;
                    nested.put(property.getName(), buildProperties(op.getValue()));
                } else {
                    nested.put(property.getName(), property.getValue());
                }
            }
        }
        return nested;
    }

    @Override
    public void formatAtomResponseEntry(EntityFeature entity,
                                        List<ParameterConfiguration> paramConfigurations,
                                        Entry entry,
                                        Gson gson,
                                        String token) {
        RegardsModule rm = new RegardsModuleImpl();
        rm.setGsonBuilder(gson);
        rm.setEntity(entity);
        entry.getModules().add(rm);
    }

    @Override
    public Optional<String> getDescriptorParameterValue(DescriptionParameter descParameter) {
        if (isRegardsExtensionAttribute(descParameter.getConfiguration())) {
            return Optional.of(String.format("{%s:%s}", REGARDS_NS, descParameter.getName()));
        }
        return Optional.empty();
    }

    @Override
    public List<OpenSearchParameter> getDescriptorBasicExtensionParameters() {
        return Collections.emptyList();
    }

    @Override
    public void applyToDescription(OpenSearchDescription openSearchDescription) {
        // Nothing to do
    }

    @Override
    protected ICriterion buildCriteria(SearchParameter parameter) throws ExtensionException {
        try {
            return AttributeCriterionBuilder.build(parameter.getAttributeModel(),
                                                   ParameterOperator.EQ,
                                                   parameter.getSearchValues());
        } catch (UnsupportedCriterionOperator e) {
            throw new ExtensionException(e);
        }
    }

    @Override
    protected boolean supportsSearchParameter(SearchParameter parameter) {
        // REGARDS extension supports parameter when :
        // Parameter is an attribute from the AttributeModel class of models.
        // There is no configuration for this parameter in the opensearch parameters configuration or if the configuration namespace is regards
        return (parameter.getAttributeModel() != null) && isRegardsExtensionAttribute(parameter.getConfiguration());
    }

    private boolean isRegardsExtensionAttribute(ParameterConfiguration configuration) {
        return (configuration == null) || (configuration.getNamespace() == null)
            || REGARDS_NS.equals(configuration.getNamespace());
    }

}
