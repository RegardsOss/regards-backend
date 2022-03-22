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
package fr.cnes.regards.modules.indexer.dao.builder;

import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.indexer.dao.spatial.builders.CircleBuilder;
import fr.cnes.regards.modules.indexer.dao.spatial.builders.CoordinatesBuilder;
import fr.cnes.regards.modules.indexer.dao.spatial.builders.EnvelopeBuilder;
import fr.cnes.regards.modules.indexer.dao.spatial.builders.PolygonBuilder;
import fr.cnes.regards.modules.indexer.domain.criterion.*;
import org.elasticsearch.geometry.Geometry;
import org.elasticsearch.index.query.*;
import org.locationtech.jts.geom.Coordinate;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 *
 * Criterion visitor implementation to generate Elasticsearch QueryBuilder from
 * a search criterion
 *
 * @author oroussel
 */
public class QueryBuilderCriterionVisitor implements ICriterionVisitor<QueryBuilder> {

    private static final Double WEST_DATELINE = -180.0;

    private static final Double EAST_DATELINE = 180.0;

    private static final Double MAX_X_EXTENT = 360.0;

    private static final Double NORTH_LIMIT = 90.0;

    private static final Double SOUTH_LIMIT = -90.0;

    private static final Double WEST_LIMIT = -360.0;

    private static final Double EAST_LIMIT = 360.0;

    private static final Double RELOCATE_DATELINE = 360.0;

    @Override
    public QueryBuilder visitEmptyCriterion(EmptyCriterion criterion) {
        return QueryBuilders.matchAllQuery();
    }

    @Override
    public QueryBuilder visitAndCriterion(AbstractMultiCriterion criterion) {
        BoolQueryBuilder andQueryBuilder = QueryBuilders.boolQuery();
        for (ICriterion embedded : criterion.getCriterions()) {
            andQueryBuilder.must(embedded.accept(this));
        }
        return andQueryBuilder;
    }

    @Override
    public QueryBuilder visitOrCriterion(AbstractMultiCriterion criterion) {
        BoolQueryBuilder orQueryBuilder = QueryBuilders.boolQuery();
        for (ICriterion embedded : criterion.getCriterions()) {
            orQueryBuilder.should(embedded.accept(this));
        }
        return orQueryBuilder;
    }

    @Override
    public QueryBuilder visitNotCriterion(NotCriterion criterion) {
        return QueryBuilders.boolQuery().mustNot(criterion.getCriterion().accept(this));
    }

    /**
     * Build sensitive or insensitive search matching
     *
     * @see StringMatchCriterion for explanations
     */
    @Override
    public QueryBuilder visitStringMatchCriterion(StringMatchCriterion criterion) {
        switch (criterion.getMatchType()) {
            case KEYWORD:
                return visitStringMatchCriterion(criterion, ".keyword");
            case FULL_TEXT_SEARCH:
                return visitStringMatchCriterion(criterion, "");
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported string match type %s", criterion.getMatchType()));
        }
    }

    protected QueryBuilder visitStringMatchCriterion(StringMatchCriterion criterion, String searchIndexSuffix) {
        String searchValue = criterion.getValue();
        String attName = criterion.getName();

        switch (criterion.getType()) {
            case EQUALS:
                // attribute type is declared by hand so there is no corresponding keyword field.
                if (attName.equals("type")) {
                    return QueryBuilders.matchPhraseQuery(attName, searchValue);
                } else {
                    return QueryBuilders.matchPhraseQuery(attName + searchIndexSuffix, searchValue);
                }
            case STARTS_WITH:
                return QueryBuilders.matchPhrasePrefixQuery(attName, searchValue).maxExpansions(10_000);
            case ENDS_WITH:
                return QueryBuilders.regexpQuery(attName + searchIndexSuffix, ".*" + escape(searchValue));
            case CONTAINS:
                return QueryBuilders.regexpQuery(attName + searchIndexSuffix, ".*" + escape(searchValue) + ".*");
            case REGEXP:
                return QueryBuilders.regexpQuery(attName + searchIndexSuffix, searchValue);
            default:
                return null;
        }
    }

    private String escape(String searchValue) {
        return "\"" + searchValue + "\"";
    }

    @Override
    public QueryBuilder visitStringMultiMatchCriterion(StringMultiMatchCriterion criterion) {
        String searchValue = criterion.getValue();
        Set<String> attNames = criterion.getNames();
        MultiMatchQueryBuilder builder = QueryBuilders
                .multiMatchQuery(searchValue, attNames.toArray(new String[attNames.size()]));
        builder.type(criterion.getType());
        return builder;
    }

    @Override
    public QueryBuilder visitStringMatchAnyCriterion(StringMatchAnyCriterion criterion){
        switch (criterion.getMatchType()) {
            case KEYWORD:
                return QueryBuilders.termsQuery(criterion.getName() + ".keyword", criterion.getValue());
            case FULL_TEXT_SEARCH:
                return QueryBuilders.matchQuery(criterion.getName(), criterion.getValue());
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported string match type %s", criterion.getMatchType()));
        }
    }

    @Override
    public QueryBuilder visitIntMatchCriterion(IntMatchCriterion criterion) {
        return QueryBuilders.termQuery(criterion.getName(), criterion.getValue());
    }

    @Override
    public QueryBuilder visitLongMatchCriterion(LongMatchCriterion criterion) {
        return QueryBuilders.termQuery(criterion.getName(), criterion.getValue());
    }

    @Override
    public QueryBuilder visitDateMatchCriterion(DateMatchCriterion criterion) {
        return QueryBuilders.termQuery(criterion.getName(), criterion.getValue());
    }

    @Override
    public <U extends Comparable<? super U>> QueryBuilder visitRangeCriterion(RangeCriterion<U> criterion) {
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(criterion.getName());

        for (ValueComparison<U> valueComp : criterion.getValueComparisons()) {
            U value = valueComp.getValue();
            switch (valueComp.getOperator()) {
                case GREATER:
                    rangeQueryBuilder.gt(value);
                    break;
                case GREATER_OR_EQUAL:
                    rangeQueryBuilder.gte(value);
                    break;
                case LESS:
                    rangeQueryBuilder.lt(value);
                    break;
                case LESS_OR_EQUAL:
                    rangeQueryBuilder.lte(value);
                    break;
                default:
            }
        }
        return rangeQueryBuilder;
    }

    @Override
    public QueryBuilder visitDateRangeCriterion(DateRangeCriterion criterion) {
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery(criterion.getName());

        for (ValueComparison<OffsetDateTime> valueComp : criterion.getValueComparisons()) {
            OffsetDateTime date = valueComp.getValue();
            switch (valueComp.getOperator()) {
                case GREATER:
                    rangeQueryBuilder.gt(OffsetDateTimeAdapter.format(date));
                    break;
                case GREATER_OR_EQUAL:
                    rangeQueryBuilder.gte(OffsetDateTimeAdapter.format(date));
                    break;
                case LESS:
                    rangeQueryBuilder.lt(OffsetDateTimeAdapter.format(date));
                    break;
                case LESS_OR_EQUAL:
                    rangeQueryBuilder.lte(OffsetDateTimeAdapter.format(date));
                    break;
                default:
            }
        }
        return rangeQueryBuilder;
    }

    @Override
    public QueryBuilder visitBooleanMatchCriterion(BooleanMatchCriterion criterion) {
        return QueryBuilders.termQuery(criterion.getName(), criterion.getValue());
    }

    @Override
    public QueryBuilder visitCircleCriterion(CircleCriterion criterion) {
        double[] center = criterion.getCoordinates();
        try {
            CircleBuilder circleBuilder = new CircleBuilder();
            circleBuilder.center(center[0], center[1]);
            circleBuilder.radius(criterion.getRadius());
            return QueryBuilders.geoIntersectionQuery(IMapping.GEO_SHAPE_ATTRIBUTE, circleBuilder.buildGeometry());
        } catch (IOException ioe) { // Never occurs
            throw new RsRuntimeException(ioe);
        }
    }

    @Override
    public QueryBuilder visitFieldExistsCriterion(FieldExistsCriterion criterion) {
        return QueryBuilders.existsQuery(criterion.getName());
    }

    @Override
    public QueryBuilder visitPolygonCriterion(PolygonCriterion criterion) {
        try {
            return QueryBuilders
                    .geoIntersectionQuery(IMapping.GEO_SHAPE_ATTRIBUTE, buildGeometryFromCoordinates(criterion.getCoordinates()));
        } catch (IOException ioe) { // Never occurs
            throw new RsRuntimeException(ioe);
        }
    }

    /**
     * Build Geometry using the coordinates with a PolygonBuilder
     * @param coordinates the coordinates of the Geometry
     * @return the Geometry
     */
    private Geometry buildGeometryFromCoordinates(double[][][] coordinates) {
        CoordinatesBuilder coordinatesBuilder = new CoordinatesBuilder();
        for (int i = 0; i < coordinates[0].length; i++) {
            coordinatesBuilder.coordinate(new Coordinate(coordinates[0][i][0], coordinates[0][i][1]));
        }
        PolygonBuilder builder = new PolygonBuilder(coordinatesBuilder);
        return builder.buildGeometry();
    }

    @Override
    public QueryBuilder visitBoundaryBoxCriterion(BoundaryBoxCriterion criterion) {

        // API manages longitude from -360 to 360
        // Check X constraints
        if (criterion.getMinX() >= criterion.getMaxX()) {
            String message = String
                    .format("MinX must be less than MaxX : %s < %s", criterion.getMinX(), criterion.getMaxX());
            throw new RsRuntimeException(message);
        }
        // Check Y constraints
        if (criterion.getMinY() >= criterion.getMaxY()) {
            String message = String
                    .format("MinY must be less than MaxY : %s < %s", criterion.getMinY(), criterion.getMaxY());
            throw new RsRuntimeException(message);
        }
        checkYLimits(criterion.getMinY());
        checkYLimits(criterion.getMaxY());

        if (criterion.getMaxX() - criterion.getMinX() >= MAX_X_EXTENT) {
            // EAST - WEST >= 360
            // bbox reaches the max extent : relocate into single bbox with max longitude extent
            criterion.setMinX(WEST_DATELINE);
            criterion.setMaxX(EAST_DATELINE);
            return getEnvelope(criterion);
        } else if (criterion.getMinX() < WEST_LIMIT) {
            // bbox is out of bound minX < -360 : translate +d*360, where D=Int(WEST/360)
            long ratio = Math.abs(Math.round(criterion.getMinX() / MAX_X_EXTENT));
            criterion.setMinX(criterion.getMinX() + ratio * MAX_X_EXTENT);
            criterion.setMaxX(criterion.getMaxX() + ratio * MAX_X_EXTENT);
            return visitBoundaryBoxCriterion(criterion);
        } else if (criterion.getMaxX() > EAST_LIMIT) {
            // bbox is out of bound maxX > 360 : translate -d*360, where D=Int(EAST/360)
            long ratio = Math.abs(Math.round(criterion.getMaxX() / MAX_X_EXTENT));
            criterion.setMinX(criterion.getMinX() - ratio * MAX_X_EXTENT);
            criterion.setMaxX(criterion.getMaxX() - ratio * MAX_X_EXTENT);
            return visitBoundaryBoxCriterion(criterion);
        } else if (criterion.getMinX() > EAST_DATELINE) {
            // bbox is between +180 and +360 : relocate it as single bbox
            criterion.setMinX(criterion.getMinX() - RELOCATE_DATELINE);
            criterion.setMaxX(criterion.getMaxX() - RELOCATE_DATELINE);
            return getEnvelope(criterion);
        } else if (criterion.getMaxX() < WEST_DATELINE) {
            // bbox is between -360 and -180 : relocate it as single bbox
            criterion.setMinX(criterion.getMinX() + RELOCATE_DATELINE);
            criterion.setMaxX(criterion.getMaxX() + RELOCATE_DATELINE);
            return getEnvelope(criterion);
        } else if (criterion.getMinX() >= WEST_DATELINE && criterion.getMaxX() > EAST_DATELINE) {
            // East crossing bbox
            // bbox is between -180 and +360 : cut it into 2 bbox with relocation
            return ICriterion.or(ICriterion.intersectsBbox(criterion.getMinX(), criterion.getMinY(), EAST_DATELINE,
                                                           criterion.getMaxY()), ICriterion
                                         .intersectsBbox(WEST_DATELINE, criterion.getMinY(),
                                                         criterion.getMaxX() - RELOCATE_DATELINE, criterion.getMaxY()))
                    .accept(this);
        } else if (criterion.getMinX() < WEST_DATELINE && criterion.getMaxX() <= EAST_DATELINE) {
            // West crossing bbox
            // bbox is between -360 and +180 : cut it into 2 bbox with relocation
            return ICriterion.or(ICriterion.intersectsBbox(WEST_DATELINE, criterion.getMinY(), criterion.getMaxX(),
                                                           criterion.getMaxY()), ICriterion
                                         .intersectsBbox(criterion.getMinX() + RELOCATE_DATELINE, criterion.getMinY(),
                                                         EAST_DATELINE, criterion.getMaxY())).accept(this);

        } else {
            // bbox is between -180 and +180 : no crossed dateline / basic case
            return getEnvelope(criterion);
        }
    }

    private void checkYLimits(double value) {
        if (value < SOUTH_LIMIT || value > NORTH_LIMIT) {
            String message = String.format("Y value must be between %s and %s : %s", SOUTH_LIMIT, NORTH_LIMIT, value);
            throw new RsRuntimeException(message);
        }
    }

    private QueryBuilder getEnvelope(BoundaryBoxCriterion boundaryBoxCriterion) {
        try {
            // upper left, lower right
            // (minX, maxY), (maxX, minY)
            EnvelopeBuilder envelopeBuilder = new EnvelopeBuilder(
                    new Coordinate(boundaryBoxCriterion.getMinX(), boundaryBoxCriterion.getMaxY()),
                    new Coordinate(boundaryBoxCriterion.getMaxX(), boundaryBoxCriterion.getMinY()));
            return QueryBuilders.geoIntersectionQuery(IMapping.GEO_SHAPE_ATTRIBUTE, envelopeBuilder.buildGeometry());
        } catch (IOException ioe) {
            throw new RsRuntimeException(ioe);
        }
    }
}
