/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.base.Joiner;
import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.indexer.dao.spatial.GeoQueries;
import fr.cnes.regards.modules.indexer.domain.criterion.*;
import org.elasticsearch.common.geo.builders.CircleBuilder;
import org.elasticsearch.common.geo.builders.EnvelopeBuilder;
import org.elasticsearch.index.query.*;
import org.locationtech.jts.geom.Coordinate;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Criterion visitor implementation to generate Elasticsearch QueryBuilder from
 * a search criterion
 * @author oroussel
 */
public class QueryBuilderCriterionVisitor implements ICriterionVisitor<QueryBuilder> {

    /**
     * Text subfield mapping used for string search criterions
     */
    private static final String KEYWORD = ".keyword";

    @Override
    public QueryBuilder visitEmptyCriterion(EmptyCriterion criterion) {
        return QueryBuilders.matchAllQuery();
    }

    @Override
    public QueryBuilder visitAndCriterion(AbstractMultiCriterion criterion) {
        BoolQueryBuilder andQueryBuilder = QueryBuilders.boolQuery();
        for (ICriterion crit : criterion.getCriterions()) {
            andQueryBuilder.must(crit.accept(this));
        }
        return andQueryBuilder;
    }

    @Override
    public QueryBuilder visitOrCriterion(AbstractMultiCriterion criterion) {
        BoolQueryBuilder orQueryBuilder = QueryBuilders.boolQuery();
        for (ICriterion crit : criterion.getCriterions()) {
            orQueryBuilder.should(crit.accept(this));
        }
        return orQueryBuilder;
    }

    @Override
    public QueryBuilder visitNotCriterion(NotCriterion criterion) {
        return QueryBuilders.boolQuery().mustNot(criterion.getCriterion().accept(this));
    }

    @Override
    public QueryBuilder visitStringMatchCriterion(StringMatchCriterion criterion) {
        String searchValue = criterion.getValue();
        String attName = criterion.getName();
        switch (criterion.getType()) {
            case EQUALS:
                // attribute type is declared by hand so there is no corresponding keyword field.
                if (attName.equals("type")) {
                    return QueryBuilders.matchPhraseQuery(attName, searchValue);
                } else {
                    return QueryBuilders.matchPhraseQuery(attName + KEYWORD, searchValue);
                }
            case STARTS_WITH:
                return QueryBuilders.matchPhrasePrefixQuery(attName, searchValue).maxExpansions(10_000);
            case ENDS_WITH:
                return QueryBuilders.regexpQuery(attName + KEYWORD, ".*" + escape(searchValue));
            case CONTAINS:
                return QueryBuilders.regexpQuery(attName + KEYWORD, ".*" + escape(searchValue) + ".*");
            case REGEXP:
                return QueryBuilders.regexpQuery(attName + KEYWORD, searchValue);
            default:
                return null;
        }
    }

    private String escape(String searchValue) {
        return "\""+searchValue+"\"";
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
    public QueryBuilder visitStringMatchAnyCriterion(StringMatchAnyCriterion criterion) {
        return QueryBuilders.matchQuery(criterion.getName(), Joiner.on(" ").join(criterion.getValue()));
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
            return QueryBuilders.geoIntersectionQuery(IMapping.GEO_SHAPE_ATTRIBUTE, circleBuilder);
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
                    .geoIntersectionQuery(IMapping.GEO_SHAPE_ATTRIBUTE, GeoQueries.computeShapeBuilder(criterion));
        } catch (IOException ioe) { // Never occurs
            throw new RsRuntimeException(ioe);
        }
    }

    @Override
    public QueryBuilder visitBoundaryBoxCriterion(BoundaryBoxCriterion criterion) {
        // Manage case when maxX > 180 => 360 - minX (this case can occur for a bbox crossing dateline)
        if (criterion.getMaxX() > 180) {
            criterion.setMaxX(criterion.getMaxX() - 360.0);
        }
        // Manage case when minLon > MaxLon (ie crossing dateline) (if MaxLon is < 0)
        if (criterion.getMaxX() < 0 && criterion.getMinX() > criterion.getMaxX()) {
            // Cut BoundaryBoxCriterion into 2 BoundaryBoxCriterion, dateLine west and dateLine east
            return ICriterion
                    .or(ICriterion.intersectsBbox(criterion.getMinX(), criterion.getMinY(), 180.0, criterion.getMaxY()),
                        ICriterion
                                .intersectsBbox(-180.0, criterion.getMinY(), criterion.getMaxX(), criterion.getMaxY()))
                    .accept(this);
        }
        try {
            // upper left, lower right
            // (minX, maxY), (maxX, minY)
            EnvelopeBuilder envelopeBuilder = new EnvelopeBuilder(new Coordinate(criterion.getMinX(),
                                                                                 criterion.getMaxY()),
                                                                  new Coordinate(criterion.getMaxX(),
                                                                                 criterion.getMinY()));
            return QueryBuilders.geoIntersectionQuery(IMapping.GEO_SHAPE_ATTRIBUTE, envelopeBuilder);
        } catch (IOException ioe) {
            throw new RsRuntimeException(ioe);
        }
    }

}
