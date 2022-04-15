/*
 * Copyright 2021-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.eo;

import com.google.gson.Gson;
import com.rometools.rome.feed.atom.Entry;
import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
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
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.eo.EOModule;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.eo.impl.EOModuleImpl;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.geojson.GeoJsonEarthObservationPropertiesBuilder;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Earth Observation (EO) parameter extension for Opensearch standard.
 *
 * @author Léo Mieulet
 * @see <a href="http://docs.opengeospatial.org/is/13-026r9/13-026r9.html">XML Opensearch Earth Observation extension</a>
 * @see <a href="https://docs.ogc.org/is/17-047r1/17-047r1.html#70">GeoJSON Opensearch Earth Observation extension</a>
 */
public class EarthObservationExtension extends AbstractExtension {

    public static final String EO_NAMESPACE = "eo";

    private static final String PARAMETER_VALUE_PATTERN = "{%s:%s}";

    private static final Logger LOGGER = LoggerFactory.getLogger(EarthObservationExtension.class);

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
        return false;
    }

    @Override
    public void formatAtomResponseEntry(EntityFeature entity,
                                        List<ParameterConfiguration> paramConfigurations,
                                        Entry entry,
                                        Gson gson,
                                        String scope) {
        Map<EarthObservationAttribute, Object> activeProperties = getActiveProperties(entity, paramConfigurations);
        if (!activeProperties.isEmpty()) {
            // Add EarthObservation module to handle this extension
            EOModule eo = new EOModuleImpl();
            eo.setGsonBuilder(gson);
            eo.setActiveProperties(activeProperties);
            entry.getModules().add(eo);
        }
    }

    @Override
    public void formatGeoJsonResponseFeature(EntityFeature entity,
                                             List<ParameterConfiguration> paramConfigurations,
                                             Feature feature,
                                             String token) {
        Map<EarthObservationAttribute, Object> mappedValues = getActiveProperties(entity, paramConfigurations);
        feature.getProperties().putAll(GeoJsonEarthObservationPropertiesBuilder.buildProperties(mappedValues));
    }

    @Override
    public Optional<String> getDescriptorParameterValue(DescriptionParameter descParameter) {
        ParameterConfiguration conf = descParameter.getConfiguration();
        if ((conf != null) && isEOConfiguration(conf)) {
            return Optional.of(String.format(PARAMETER_VALUE_PATTERN, EO_NAMESPACE, conf.getName()));
        }
        return Optional.empty();
    }

    /**
     * @return true when the configuration is related to this plugin
     */
    private boolean isEOConfiguration(ParameterConfiguration conf) {
        return EO_NAMESPACE.equals(conf.getNamespace()) && (EarthObservationAttribute.exists(conf.getName()));
    }

    @Override
    public void applyToDescription(OpenSearchDescription openSearchDescription) {
        // Nothing to do
    }

    @Override
    public List<OpenSearchParameter> getDescriptorBasicExtensionParameters() {
        return Lists.newArrayList();
    }

    private Map<EarthObservationAttribute, Object> getActiveProperties(EntityFeature entity,
                                                                       List<ParameterConfiguration> paramConfigurations) {
        Map<EarthObservationAttribute, Object> activeProperties = new HashMap<>();
        for (ParameterConfiguration paramConfiguration : paramConfigurations) {
            Optional<EarthObservationAttribute> attributeOpt = EarthObservationAttribute.fromName(paramConfiguration.getName());
            if (attributeOpt.isPresent()) {
                Optional<Object> entityPropertyValueOpt = getEntityPropertyValue(entity, paramConfiguration);
                if (entityPropertyValueOpt.isPresent()) {
                    activeProperties.put(attributeOpt.get(), entityPropertyValueOpt.get());
                }
            }
        }
        return activeProperties;
    }
}
