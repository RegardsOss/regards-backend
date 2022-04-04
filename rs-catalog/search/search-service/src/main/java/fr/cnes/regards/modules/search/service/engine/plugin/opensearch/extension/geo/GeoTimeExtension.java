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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.geo;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.rometools.modules.georss.geometries.*;
import com.rometools.rome.feed.atom.Entry;
import com.rometools.rome.feed.module.Module;
import fr.cnes.regards.framework.geojson.Feature;
import fr.cnes.regards.framework.geojson.coordinates.Positions;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.geojson.geometry.LineString;
import fr.cnes.regards.framework.geojson.geometry.Point;
import fr.cnes.regards.framework.geojson.geometry.Polygon;
import fr.cnes.regards.modules.dam.domain.entities.StaticProperties;
import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.exception.InvalidGeometryException;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
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
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.gml.impl.GmlTimeModuleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Geo&Time parameter extension for Opensearch standard.
 *
 * @author Sébastien Binda
 * @see <a href="http://www.opensearch.org/Specifications/OpenSearch/Extensions/Parameter/1.0/Draft_2">Opensearch
 * parameter extension</a>
 * @see <a href="http://www.opengeospatial.org/standards/opensearchgeo">Opensearch Geo&Time extension</a>
 */
public class GeoTimeExtension extends AbstractExtension {

    public static final String TIME_NS = "time";

    public static final String TIME_START_PARAMETER = "start";

    public static final String TIME_END_PARAMETER = "end";

    public static final List<String> TIME_PARAMETERS = Lists.newArrayList(TIME_START_PARAMETER, TIME_END_PARAMETER);

    public static final String GEO_NS = "geo";

    public static final String GEO_PARAMETER = "geometry";

    public static final String BOX_PARAMETER = "box";

    public static final String BOX_PATTERN = "^[0-9\\.\\,\\-]*$";

    public static final String LOCATION_PARAMETER = "location";

    public static final String LON_PARAMETER = "lon";

    public static final String LAT_PARAMETER = "lat";

    public static final String RADIUS_PARAMETER = "radius";

    private static final String PARAMETER_VALUE_PATTERN = "{%s:%s}";

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoTimeExtension.class);

    @Override
    public void formatGeoJsonResponseFeature(EntityFeature entity, List<ParameterConfiguration> paramConfigurations, Feature feature, String token) {
        feature.setGeometry(entity.getGeometry());
        // The normalized geometry is only used during ES research and should not be returned inside OpenSearch results
        feature.setNormalizedGeometry(null);
    }

    @Override
    public void formatAtomResponseEntry(EntityFeature entity, List<ParameterConfiguration> paramConfigurations, Entry entry, Gson gson, String scope) {
        // Add module generator
        entry.getModules().add(getAtomEntityResponseBuilder(entity, paramConfigurations, gson));

    }

    @Override
    public Optional<String> getDescriptorParameterValue(DescriptionParameter descParameter) {
        ParameterConfiguration conf = descParameter.getConfiguration();
        if ((conf != null) && isGeoTimeConfiguration(conf)) {
            return Optional.of(String.format(PARAMETER_VALUE_PATTERN, TIME_NS, conf.getName()));
        }
        return Optional.empty();
    }

    @Override
    public List<OpenSearchParameter> getDescriptorBasicExtensionParameters() {
        List<OpenSearchParameter> geoParameters = Lists.newArrayList();
        geoParameters.add(builderParameter(GEO_PARAMETER, String.format(PARAMETER_VALUE_PATTERN, GEO_NS, GEO_PARAMETER),
                                           "Defined in Well Known Text standard (WKT) with coordinates in decimal degrees (EPSG:4326)", null));
        geoParameters.add(builderParameter(BOX_PARAMETER, String.format(PARAMETER_VALUE_PATTERN, GEO_NS, BOX_PARAMETER),
                                           "Defined by 'west, south, east, north' coordinates of longitude, latitude, in decimal degrees (EPSG:4326)",
                                           BOX_PATTERN));
        // To implement
        // geoParameters.add(builderParameter(LOCATION_PARAMETER, String.format("{%s:%s}", GEO_NS, LOCATION_PARAMETER),
        // "Location string e.g. Paris, France", null));
        geoParameters.add(builderParameter(LON_PARAMETER, String.format(PARAMETER_VALUE_PATTERN, GEO_NS, LON_PARAMETER),
                                           "Longitude expressed in decimal degrees (EPSG:4326) - should be used with geo:lat", null, "180", "-180"));
        geoParameters.add(builderParameter(LAT_PARAMETER, String.format(PARAMETER_VALUE_PATTERN, GEO_NS, LAT_PARAMETER),
                                           "Latitude expressed in decimal degrees (EPSG:4326) - should be used with geo:lon", null, "90", "-90"));
        geoParameters.add(builderParameter(RADIUS_PARAMETER, String.format(PARAMETER_VALUE_PATTERN, GEO_NS, RADIUS_PARAMETER),
                                           "Latitude expressed in decimal degrees (EPSG:4326) - should be used with geo:lon", null, null, "1"));
        return geoParameters;
    }

    @Override
    public void applyToDescription(OpenSearchDescription openSearchDescription) {
        // Nothing to do
    }

    @Override
    protected ICriterion buildCriteria(SearchParameter parameter) throws ExtensionException {
        throw new ExtensionException("Geo & Time criterion cannot be build individually for each parameter.");
    }

    @Override
    protected ICriterion buildSupportedParametersCriterion(List<SearchParameter> parameters) throws ExtensionException {
        List<SearchParameter> geoParameters = Lists.newArrayList();
        List<SearchParameter> timeParameters = Lists.newArrayList();
        for (SearchParameter parameter : parameters) {
            if (parameter.getConfiguration() != null) {
                switch (parameter.getConfiguration().getNamespace()) {
                    case GEO_NS:
                        geoParameters.add(parameter);
                        break;
                    case TIME_NS:
                        timeParameters.add(parameter);
                        break;
                    default:
                        // nothing to do
                        break;
                }
            } else {
                switch (parameter.getName()) {
                    case GEO_PARAMETER:
                    case BOX_PARAMETER:
                    case LAT_PARAMETER:
                    case LON_PARAMETER:
                    case RADIUS_PARAMETER:
                        geoParameters.add(parameter);
                        break;
                    default:
                        //nothing to do
                        break;
                }
            }
        }
        return ICriterion.and(buildGeoCriterion(geoParameters), buildTimeCriterion(timeParameters));
    }

    private ICriterion buildTimeCriterion(List<SearchParameter> timeParameters) {
        List<ICriterion> criterion = Lists.newArrayList();
        Optional<SearchParameter> startParam = timeParameters.stream().filter(p -> p.getConfiguration().getName().equals(TIME_START_PARAMETER)).findFirst();
        Optional<SearchParameter> endParam = timeParameters.stream().filter(p -> p.getConfiguration().getName().equals(TIME_END_PARAMETER)).findFirst();
        if (startParam.isPresent()) {
            try {
                criterion.add(AttributeCriterionBuilder.build(startParam.get().getAttributeModel(), ParameterOperator.GE, startParam.get().getSearchValues()));
            } catch (UnsupportedCriterionOperator e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }
        if (endParam.isPresent()) {
            try {
                criterion.add(AttributeCriterionBuilder.build(endParam.get().getAttributeModel(), ParameterOperator.LE, endParam.get().getSearchValues()));
            } catch (UnsupportedCriterionOperator e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }
        return criterion.isEmpty() ? ICriterion.all() : ICriterion.and(criterion);
    }

    private ICriterion buildGeoCriterion(List<SearchParameter> geoParameters) throws ExtensionException {
        List<ICriterion> criterion = Lists.newArrayList();
        Optional<SearchParameter> geometryParameter = geoParameters.stream().filter(p -> p.getName().equals(GEO_PARAMETER)).findFirst();
        Optional<SearchParameter> lonParameter = geoParameters.stream().filter(p -> p.getName().equals(LON_PARAMETER)).findFirst();
        Optional<SearchParameter> latParameter = geoParameters.stream().filter(p -> p.getName().equals(LAT_PARAMETER)).findFirst();
        Optional<SearchParameter> radiusParameter = geoParameters.stream().filter(p -> p.getName().equals(RADIUS_PARAMETER)).findFirst();
        Optional<SearchParameter> boxParameter = geoParameters.stream().filter(p -> p.getName().equals(BOX_PARAMETER)).findFirst();

        try {
            if (geometryParameter.isPresent()) {
                criterion.add(AttributeCriterionBuilder.buildGeometryWKT(geometryParameter.get().getSearchValues()));
            }

            if (boxParameter.isPresent()) {
                criterion.add(AttributeCriterionBuilder.buildGeometryBbox(boxParameter.get().getSearchValues()));
            }

            if (lonParameter.isPresent() && latParameter.isPresent() && radiusParameter.isPresent()) {
                criterion.add(AttributeCriterionBuilder.buildGeometryCircle(lonParameter.get().getSearchValues(), latParameter.get().getSearchValues(),
                                                                            radiusParameter.get().getSearchValues()));
            }
        } catch (InvalidGeometryException e) {
            throw new ExtensionException(e);
        }

        return criterion.isEmpty() ? ICriterion.all() : ICriterion.and(criterion);
    }

    @Override
    protected boolean supportsSearchParameter(SearchParameter parameter) {
        boolean supports = parameter.getName().equals(GEO_PARAMETER) || parameter.getName().equals(BOX_PARAMETER);
        supports |= parameter.getName().equals(LON_PARAMETER) || parameter.getName().equals(LAT_PARAMETER);
        supports |= parameter.getName().equals(RADIUS_PARAMETER) || ((parameter.getConfiguration() != null) && TIME_NS.equals(
                parameter.getConfiguration().getNamespace()));
        return supports;
    }

    private Module getAtomEntityResponseBuilder(EntityFeature entity, List<ParameterConfiguration> paramConfigurations, Gson gson) {
        // Add GML with time module to handle geo & time extension
        GmlTimeModuleImpl gmlMod = new GmlTimeModuleImpl();
        ParameterConfiguration timeStartParameterConf = paramConfigurations.stream()
                .filter(c -> TIME_NS.equals(c.getNamespace()) && TIME_START_PARAMETER.equals(c.getName())).findFirst().orElse(null);
        ParameterConfiguration timeEndParameterConf = paramConfigurations.stream()
                .filter(c -> TIME_NS.equals(c.getNamespace()) && TIME_END_PARAMETER.equals(c.getName())).findFirst().orElse(null);
        if ((timeStartParameterConf != null) && (timeEndParameterConf != null)) {
            String startDateJsonPath = timeStartParameterConf.getAttributeModelJsonPath().replace(StaticProperties.FEATURE_PROPERTIES + ".", "");
            String endDateJsonPath = timeStartParameterConf.getAttributeModelJsonPath().replace(StaticProperties.FEATURE_PROPERTIES + ".", "");
            IProperty<?> startDate = entity.getProperty(startDateJsonPath);
            IProperty<?> stopDate = entity.getProperty(endDateJsonPath);
            if ((startDate != null) && (startDate.getValue() instanceof OffsetDateTime) && (stopDate != null)
                    && (stopDate.getValue() instanceof OffsetDateTime)) {
                gmlMod.setStartDate(IProperty.toDateValue(startDate.getValue()));
                gmlMod.setStopDate(IProperty.toDateValue(stopDate.getValue()));
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
                Point rp = (Point) geometry;
                com.rometools.modules.georss.geometries.Point point = new com.rometools.modules.georss.geometries.Point();
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
                Polygon p = (Polygon) geometry;
                com.rometools.modules.georss.geometries.Polygon polygon = new com.rometools.modules.georss.geometries.Polygon();

                Positions exteriorRings = p.getCoordinates().getExteriorRing();
                PositionList polygonPosList = new PositionList();
                exteriorRings.forEach(pos -> polygonPosList.add(pos.getLatitude(), pos.getLongitude()));
                LinearRing linearRing = new LinearRing(polygonPosList);
                polygon.setExterior(linearRing);

                List<Positions> interiorRings = p.getCoordinates().getHoles();
                List<AbstractRing> irs = Lists.newArrayList();
                for (Positions interiorRing : interiorRings) {
                    PositionList posList = new PositionList();
                    interiorRing.forEach(pos -> posList.add(pos.getLatitude(), pos.getLongitude()));
                    LinearRing lr = new LinearRing(posList);
                    irs.add(lr);
                }
                polygon.setInterior(irs);
                return polygon;
            case MULTILINESTRING:
            case MULTIPOINT:
            case MULTIPOLYGON:
            case GEOMETRY_COLLECTION:
                LOGGER.warn("Geometry type {} is not handled for Georss format", geometry.getType());
                return null;
            case FEATURE:
            case FEATURE_COLLECTION:
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

    private OpenSearchParameter builderParameter(String name, String value, String title, String pattern, String maxInc, String minInc) {
        OpenSearchParameter param = new OpenSearchParameter();
        param.setName(name);
        param.setValue(value);
        param.setTitle(title);
        param.setPattern(pattern);
        param.setMaxInclusive(maxInc);
        param.setMinInclusive(minInc);
        return param;
    }

    /**
     * @return true when the configuration is related to this extension
     */
    private boolean isGeoTimeConfiguration(ParameterConfiguration conf) {
        return TIME_NS.equals(conf.getNamespace()) && TIME_PARAMETERS.contains(conf.getName());
    }
}
