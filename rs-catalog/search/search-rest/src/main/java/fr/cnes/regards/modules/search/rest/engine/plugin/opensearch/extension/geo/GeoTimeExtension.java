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

import org.apache.commons.compress.utils.Lists;

import com.google.gson.Gson;
import com.rometools.modules.georss.geometries.AbstractGeometry;
import com.rometools.modules.georss.geometries.Point;
import com.rometools.modules.georss.geometries.Position;
import com.rometools.modules.georss.geometries.PositionList;
import com.rometools.rome.feed.atom.Entry;
import com.rometools.rome.feed.module.Module;

import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.geojson.geometry.LineString;
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

    public static final String GEO_NS = "geo";

    public static final String GEO_PARAMETER = "geometry";

    public static final String BOX_PARAMETER = "box";

    public static final String BOX_PATTERN = "^[0-9\\.\\,\\-]*$";

    public static final String LOCATION_PARAMETER = "location";

    public static final String LON_PARAMETER = "lon";

    public static final String LAT_PARAMETER = "lat";

    public static final String RADIUS_PARAMETER = "radius";

    @Override
    public void formatGeoJsonResponseFeature(AbstractEntity entity,
            List<OpenSearchParameterConfiguration> paramConfigurations, Feature feature) {
        feature.setGeometry(entity.getGeometry());
    }

    @Override
    public void formatAtomResponseEntry(AbstractEntity entity,
            List<OpenSearchParameterConfiguration> paramConfigurations, Entry entry, Gson gson) {
        // Add module generator
        entry.getModules().add(getAtomEntityResponseBuilder(entity, paramConfigurations, gson));

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
    }

    @Override
    public List<OpenSearchParameter> addParametersToDescription() {
        List<OpenSearchParameter> geoParameters = Lists.newArrayList();
        geoParameters
                .add(builderParameter(GEO_PARAMETER, String.format("{%s:%s}", GEO_NS, GEO_PARAMETER),
                                      "Defined in Well Known Text standard (WKT) with coordinates in decimal degrees (EPSG:4326)",
                                      null));
        geoParameters
                .add(builderParameter(BOX_PARAMETER, String.format("{%s:%s}", GEO_NS, BOX_PARAMETER),
                                      "Defined by 'west, south, east, north' coordinates of longitude, latitude, in decimal degrees (EPSG:4326)",
                                      BOX_PATTERN));
        geoParameters.add(builderParameter(LOCATION_PARAMETER, String.format("{%s:%s}", GEO_NS, LOCATION_PARAMETER),
                                           "Location string e.g. Paris, France", null));
        geoParameters
                .add(builderParameter(LON_PARAMETER, String.format("{%s:%s}", GEO_NS, LON_PARAMETER),
                                      "Longitude expressed in decimal degrees (EPSG:4326) - should be used with geo:lat",
                                      null, "180", "-180"));
        geoParameters
                .add(builderParameter(LAT_PARAMETER, String.format("{%s:%s}", GEO_NS, LAT_PARAMETER),
                                      "Latitude expressed in decimal degrees (EPSG:4326) - should be used with geo:lon",
                                      null, "90", "-90"));
        geoParameters
                .add(builderParameter(RADIUS_PARAMETER, String.format("{%s:%s}", GEO_NS, RADIUS_PARAMETER),
                                      "Latitude expressed in decimal degrees (EPSG:4326) - should be used with geo:lon",
                                      null, null, "1"));
        return geoParameters;
    }

    @Override
    public void applyToDescription(OpenSearchDescription openSearchDescription) {
        // Nothing to do
    }

    @Override
    protected ICriterion buildCriteria(SearchParameter parameter) throws UnsupportedCriterionOperator {
        ICriterion criteria = ICriterion.all();
        if (parameter.getConfiguration() != null) {
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
        } else {
            // TODO : Generate geometry criterion from geometry, box, lon, lat, location and radius.
            switch (parameter.getName()) {
                case GEO_PARAMETER:
                    break;
                case BOX_PARAMETER:
                    break;
                case LON_PARAMETER:
                    break;
                case LAT_PARAMETER:
                    break;
                case RADIUS_PARAMETER:
                default:
                    // Unknown parameter
                    break;
            }
        }

        return criteria;
    }

    @Override
    protected boolean supportsSearchParameter(SearchParameter parameter) {
        return parameter.getName().equals(GEO_PARAMETER) || parameter.getName().equals(BOX_PARAMETER)
                || parameter.getName().equals(LON_PARAMETER) || parameter.getName().equals(LAT_PARAMETER)
                || parameter.getName().equals(RADIUS_PARAMETER) || ((parameter.getConfiguration() != null)
                        && TIME_NS.equals(parameter.getConfiguration().getNamespace()));
    }

    private Module getAtomEntityResponseBuilder(AbstractEntity entity,
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
            // TODO : GEO Translation from IGEometry to rome module geometry
            case POINT:
                fr.cnes.regards.framework.geojson.geometry.Point rp = (fr.cnes.regards.framework.geojson.geometry.Point) geometry;
                Point point = new Point();
                point.setPosition(new Position(rp.getCoordinates().getLatitude(), rp.getCoordinates().getLongitude()));
                return point;
            case LINESTRING:
                LineString ls = (LineString) geometry;
                com.rometools.modules.georss.geometries.LineString lineString = new com.rometools.modules.georss.geometries.LineString();
                PositionList positionList = new PositionList();
                ls.getCoordinates().forEach(c -> positionList.add(c.getLatitude(), c.getLongitude()));
                lineString.setPositionList(positionList);
                return lineString;
            case POLYGON:
                // TODO : GEO Translation from IGEometry:POLYGON to rome module geometry POLYGON
                return null;
            case MULTILINESTRING:
            case MULTIPOINT:
            case MULTIPOLYGON:
                // TODO : Do we have to handle this kind of geometry ?
                return null;
            case FEATURE:
            case FEATURE_COLLECTION:
            case GEOMETRY_COLLECTION:
            case UNLOCATED:
            default:
                // Nothing to do
                return null;
        }
    }

    private OpenSearchParameter builderParameter(String name, String value, String title, String pattern) {
        OpenSearchParameter param = new OpenSearchParameter();
        param.setName(name);
        param.setValue(value);
        param.setTitle(title);
        param.setPattern(pattern);
        return param;
    }

    private OpenSearchParameter builderParameter(String name, String value, String title, String pattern, String maxInc,
            String minInc) {
        OpenSearchParameter param = new OpenSearchParameter();
        param.setName(name);
        param.setValue(value);
        param.setTitle(title);
        param.setPattern(pattern);
        param.setMaxInclusive(maxInc);
        param.setMinInclusive(minInc);
        return param;
    }
}
