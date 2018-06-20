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
package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.geo;

import java.time.OffsetDateTime;
import java.util.List;

import com.google.gson.Gson;
import com.rometools.modules.georss.geometries.AbstractGeometry;
import com.rometools.modules.georss.geometries.Point;
import com.rometools.modules.georss.geometries.Position;
import com.rometools.rome.feed.module.Module;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.StaticProperties;
import fr.cnes.regards.modules.entities.domain.attribute.AbstractAttribute;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.AttributeCriterionBuilder;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.OpenSearchParameterConfiguration;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.ParameterOperator;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.description.DescriptionParameter;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.exception.UnsupportedCriterionOperator;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.AbstractOpenSearchExtension;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.extension.SearchParameter;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.formatter.atom.modules.gml.impl.GmlTimeModuleImpl;
import fr.cnes.regards.modules.search.schema.OpenSearchDescription;
import fr.cnes.regards.modules.search.schema.parameters.OpenSearchParameter;

/**
 * Geo&Time parameter extension for Opensearch standard.
 * @see <a href="http://www.opensearch.org/Specifications/OpenSearch/Extensions/Parameter/1.0/Draft_2">Opensearch parameter extension</a>
 * @see <a href="http://www.opengeospatial.org/standards/opensearchgeo">Opensearch Geo&Time extension</a>
 *
 * @author Sébastien Binda
 */
public class GeoTimeExtension extends AbstractOpenSearchExtension {

    public static final String TIME_NS = "time";

    public static final String TIME_START_PARAMETER = "start";

    public static final String TIME_END_PARAMETER = "end";

    @Override
    public void formatGeoJsonResponseFeature(AbstractEntity entity,
            List<OpenSearchParameterConfiguration> paramConfigurations, Feature feature) {
        feature.setGeometry(entity.getGeometry());
        // TODO time
    }

    @Override
    public Module getAtomEntityResponseBuilder(AbstractEntity entity,
            List<OpenSearchParameterConfiguration> paramConfigurations, Gson gson) {
        // Add GML with time module to handle geo & time extension
        GmlTimeModuleImpl gmlMod = new GmlTimeModuleImpl();
        OpenSearchParameterConfiguration timeStartParameterConf = paramConfigurations.stream()
                .filter(c -> TIME_NS.equals(c.getNamespace()) && TIME_START_PARAMETER.equals(c.getName())).findFirst()
                .orElse(null);
        OpenSearchParameterConfiguration timeEndParameterConf = paramConfigurations.stream()
                .filter(c -> TIME_NS.equals(c.getNamespace()) && TIME_END_PARAMETER.equals(c.getName())).findFirst()
                .orElse(null);
        if ((timeStartParameterConf != null) && (timeEndParameterConf != null)) {
            // TODO : Handle static properties ?
            String startDateJsonPath = timeStartParameterConf.getAttributeModelJsonPath()
                    .replace(StaticProperties.PROPERTIES + ".", "");
            String endDateJsonPath = timeStartParameterConf.getAttributeModelJsonPath()
                    .replace(StaticProperties.PROPERTIES + ".", "");
            AbstractAttribute<?> startDate = entity.getProperty(startDateJsonPath);
            AbstractAttribute<?> stopDate = entity.getProperty(endDateJsonPath);
            if ((startDate != null) && (startDate.getValue() instanceof OffsetDateTime) && (stopDate != null)
                    && (stopDate.getValue() instanceof OffsetDateTime)) {
                gmlMod.setStartDate((OffsetDateTime) startDate.getValue());
                gmlMod.setStopDate((OffsetDateTime) stopDate.getValue());
            }
            gmlMod.setGsonBuilder(gson);
            gmlMod.setGeometry(buildGeometry(entity.getGeometry()));
        }
        return gmlMod;
    }

    private AbstractGeometry buildGeometry(IGeometry geometry) {
        if (geometry == null) {
            return null;
        }
        switch (geometry.getType()) {
            case POINT:
                fr.cnes.regards.framework.geojson.geometry.Point rp = (fr.cnes.regards.framework.geojson.geometry.Point) geometry;
                Point point = new Point();
                point.setPosition(new Position(rp.getCoordinates().getLatitude(), rp.getCoordinates().getLongitude()));
                return point;
            case FEATURE:
            case FEATURE_COLLECTION:
            case GEOMETRY_COLLECTION:
            case LINESTRING:
            case MULTILINESTRING:
            case MULTIPOINT:
            case MULTIPOLYGON:
            case POLYGON:
            case UNLOCATED:
            default:
                // TODO implement builders.
                return null;
        }
    }

    @Override
    public void applyToDescriptionParameter(OpenSearchParameter parameter, DescriptionParameter descParameter) {
        OpenSearchParameterConfiguration conf = descParameter.getConfiguration();
        if ((conf != null) && TIME_NS.equals(conf.getNamespace()) && TIME_START_PARAMETER.equals(conf.getName())) {
            parameter.setValue(String.format("{%s:%s}", TIME_NS, TIME_START_PARAMETER));
        }
        if ((conf != null) && TIME_NS.equals(conf.getNamespace()) && TIME_END_PARAMETER.equals(conf.getName())) {
            parameter.setValue(String.format("{%s:%s}", TIME_NS, TIME_END_PARAMETER));
        }
        // TODO Handle geometry
    }

    @Override
    public void applyToDescription(OpenSearchDescription openSearchDescription) {
        // Nothing to do
    }

    @Override
    protected ICriterion buildCriteria(SearchParameter parameter) throws UnsupportedCriterionOperator {
        ICriterion criteria = ICriterion.all();
        if (TIME_NS.equals(parameter.getConfiguration().getNamespace())
                && TIME_START_PARAMETER.equals(parameter.getConfiguration().getName())) {
            // Parse attribute value to create associated ICriterion using parameter configuration
            criteria = AttributeCriterionBuilder.build(parameter.getAttributeModel(), ParameterOperator.GE,
                                                       parameter.getSearchValues());
        }
        if (TIME_NS.equals(parameter.getConfiguration().getNamespace())
                && TIME_END_PARAMETER.equals(parameter.getConfiguration().getName())) {
            criteria = AttributeCriterionBuilder.build(parameter.getAttributeModel(), ParameterOperator.LE,
                                                       parameter.getSearchValues());
        }
        return criteria;
    }

    @Override
    protected boolean supportsSearchParameter(OpenSearchParameterConfiguration conf) {
        return (conf != null) && TIME_NS.equals(conf.getNamespace());
    }
}
