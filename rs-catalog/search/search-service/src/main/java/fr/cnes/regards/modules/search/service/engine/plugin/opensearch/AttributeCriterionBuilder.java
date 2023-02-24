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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch;

import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.modules.dam.domain.entities.criterion.IFeatureCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import fr.cnes.regards.modules.indexer.domain.criterion.exception.InvalidGeometryException;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.opensearch.service.parser.GeometryCriterionBuilder;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.exception.UnsupportedCriterionOperator;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ICriterion} builder for catalog searches.
 *
 * @author Sébastien Binda
 */
public class AttributeCriterionBuilder {

    public static final String INVALID_OPERATOR_S_FOR_STRING_PARAMETER_S = "Invalid operator %s for string parameter %s";

    private AttributeCriterionBuilder() {
    }

    /**
     * Build a {@link ICriterion} for catalog search from
     *
     * @param attributeModel {@link AttributeModel}
     * @param operator       {@link ParameterOperator} to apply for the current attribute search
     * @param values         {@link String}s search values.
     * @return {@link ICriterion}
     */
    public static ICriterion build(AttributeModel attributeModel, ParameterOperator operator, List<String> values)
        throws UnsupportedCriterionOperator {
        ICriterion criterion = null;
        for (String value : values) {
            ICriterion valueCriterion = null;
            switch (attributeModel.getType()) {
                case INTEGER:
                case INTEGER_ARRAY:
                    valueCriterion = buildIngetegerCrit(attributeModel, value, operator);
                    break;
                case DOUBLE:
                case DOUBLE_ARRAY:
                    valueCriterion = buildDoubleCrit(attributeModel, value, operator);
                    break;
                case LONG:
                case LONG_ARRAY:
                    valueCriterion = buildLongCrit(attributeModel, value, operator);
                    break;
                case STRING:
                case URL:
                    valueCriterion = buildStringCrit(attributeModel, value, operator);
                    break;
                case STRING_ARRAY:
                    valueCriterion = buildStringArrayCrit(attributeModel, value, operator);
                    break;
                case DATE_ISO8601:
                    valueCriterion = buildDateCrit(attributeModel, value, operator);
                    break;
                case BOOLEAN:
                    valueCriterion = buildBooleanCrit(attributeModel, value, operator);
                    break;
                default:
                    // Nothing to do
                    break;
            }
            if ((criterion == null) && (valueCriterion != null)) {
                criterion = valueCriterion;
            } else if (valueCriterion != null) {
                criterion = ICriterion.and(criterion, valueCriterion);
            }
        }
        return criterion;
    }

    /**
     * Build an Boolean {@link ICriterion} for catalog searches from a given search parameter and value.
     *
     * @param attribute {@link AttributeModel} to search for
     * @param value     String representation of boolean parameter value
     * @param operator  {@link ParameterOperator} for search
     * @return {@link ICriterion}
     */
    private static ICriterion buildBooleanCrit(AttributeModel attribute, String value, ParameterOperator operator)
        throws UnsupportedCriterionOperator {
        switch (operator) {
            case GE:
            case GT:
            case LE:
            case LT:
                throw new UnsupportedCriterionOperator(String.format(INVALID_OPERATOR_S_FOR_STRING_PARAMETER_S,
                                                                     operator.toString(),
                                                                     attribute));
            case EQ:
            default:
                return IFeatureCriterion.eq(attribute, Boolean.valueOf(value));
        }
    }

    /**
     * Build an Date {@link ICriterion} for catalog searches from a given search parameter and value.
     *
     * @param attribute {@link AttributeModel} to search for
     * @param value     String representation of date parameter value
     * @param operator  {@link ParameterOperator} for search
     * @return {@link ICriterion}
     */
    private static ICriterion buildDateCrit(AttributeModel attribute, String value, ParameterOperator operator) {
        switch (operator) {
            case GE:
                return IFeatureCriterion.ge(attribute, OffsetDateTimeAdapter.parse(value));
            case GT:
                return IFeatureCriterion.gt(attribute, OffsetDateTimeAdapter.parse(value));
            case LE:
                return IFeatureCriterion.le(attribute, OffsetDateTimeAdapter.parse(value));
            case LT:
                return IFeatureCriterion.lt(attribute, OffsetDateTimeAdapter.parse(value));
            case EQ:
            default:
                return IFeatureCriterion.eq(attribute, OffsetDateTimeAdapter.parse(value));
        }
    }

    /**
     * Build an Integer {@link ICriterion} for catalog searches from a given search parameter and value.
     *
     * @param attribute {@link AttributeModel} to search for
     * @param value     String representation of integer parameter value
     * @param operator  {@link ParameterOperator} for search
     * @return {@link ICriterion}
     */
    private static ICriterion buildIngetegerCrit(AttributeModel attribute, String value, ParameterOperator operator) {
        // Important :
        // We have to do it because the value of the criterion returned by Elasticsearch is always a double value,
        // even if the value is an integer value.
        // For example, it did not work, then the open search criterion was : "property:26.0"
        int val;
        try {
            val = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            Double doubleValue = Double.parseDouble(value);
            val = doubleValue.intValue();
        }
        switch (operator) {
            case GE:
                return IFeatureCriterion.ge(attribute, val);
            case GT:
                return IFeatureCriterion.gt(attribute, val);
            case LE:
                return IFeatureCriterion.le(attribute, val);
            case LT:
                return IFeatureCriterion.lt(attribute, val);
            case EQ:
            default:
                return IFeatureCriterion.eq(attribute, val);
        }
    }

    /**
     * Build an Double {@link ICriterion} for catalog searches from a given search parameter and value.
     *
     * @param attribute {@link AttributeModel} to search for
     * @param value     String representation of double parameter value
     * @param operator  {@link ParameterOperator} for search
     * @return {@link ICriterion}
     */
    private static ICriterion buildDoubleCrit(AttributeModel attribute, String value, ParameterOperator operator) {
        Double asDouble = Double.parseDouble(value);
        switch (operator) {
            case GE:
                return IFeatureCriterion.ge(attribute, asDouble);
            case GT:
                return IFeatureCriterion.gt(attribute, asDouble);
            case LE:
                return IFeatureCriterion.le(attribute, asDouble);
            case LT:
                return IFeatureCriterion.lt(attribute, asDouble);
            case EQ:
            default:
                return IFeatureCriterion.eq(attribute, asDouble, asDouble - Math.nextDown(asDouble));
        }
    }

    /**
     * Build an Long {@link ICriterion} for catalog searches from a given search parameter and value.
     *
     * @param attribute {@link AttributeModel} to search for
     * @param value     String representation of long parameter value
     * @param operator  {@link ParameterOperator} for search
     * @return {@link ICriterion}
     */
    private static ICriterion buildLongCrit(AttributeModel attribute, String value, ParameterOperator operator) {
        // Important :
        // We have to do it because the value of the criterion returned by Elasticsearch is always a double value,
        // even if the value is a long value.
        // For example, it did not work, then the open search criterion was : "property:26.0"
        long valL;
        try {
            valL = Long.parseLong(value);
        } catch (NumberFormatException ex) {
            Double doubleValue = Double.parseDouble(value);
            valL = doubleValue.longValue();
        }
        switch (operator) {
            case GE:
                return IFeatureCriterion.ge(attribute, valL);
            case GT:
                return IFeatureCriterion.gt(attribute, valL);
            case LE:
                return IFeatureCriterion.le(attribute, valL);
            case LT:
                return IFeatureCriterion.lt(attribute, valL);
            case EQ:
            default:
                return IFeatureCriterion.eq(attribute, valL);
        }
    }

    /**
     * Build an String {@link ICriterion} for catalog searches from a given search parameter and value.
     *
     * @param attribute {@link AttributeModel} to search for
     * @param value     String representation of parameter value
     * @param operator  {@link ParameterOperator} for search
     * @return {@link ICriterion}
     */
    private static ICriterion buildStringCrit(AttributeModel attribute, String value, ParameterOperator operator)
        throws UnsupportedCriterionOperator {
        switch (operator) {
            case GE:
            case GT:
            case LE:
            case LT:
                throw new UnsupportedCriterionOperator(String.format(INVALID_OPERATOR_S_FOR_STRING_PARAMETER_S,
                                                                     operator.toString(),
                                                                     attribute));
            case EQ:
            default:
                return IFeatureCriterion.eq(attribute, value, StringMatchType.KEYWORD);
        }
    }

    /**
     * Build a String {@link ICriterion} for catalog searches from a given search parameter and value.
     *
     * @param attribute {@link AttributeModel} to search for
     * @param value     String representation of parameter value
     * @param operator  {@link ParameterOperator} for search
     * @return {@link ICriterion}
     */
    private static ICriterion buildStringArrayCrit(AttributeModel attribute, String value, ParameterOperator operator)
        throws UnsupportedCriterionOperator {
        switch (operator) {
            case GE:
            case GT:
            case LE:
            case LT:
                throw new UnsupportedCriterionOperator(String.format(INVALID_OPERATOR_S_FOR_STRING_PARAMETER_S,
                                                                     operator.toString(),
                                                                     attribute));
            case EQ:
            default:
                return IFeatureCriterion.contains(attribute, value, StringMatchType.KEYWORD);
        }
    }

    /**
     * Build a {@link ICriterion} to search for the lists of geometry given in WKT format.
     *
     * @param wkts geometry to search for
     * @return {@link ICriterion}
     */
    public static ICriterion buildGeometryWKT(List<String> wkts) throws InvalidGeometryException {
        List<ICriterion> criterion = new ArrayList<>();
        for (String wktGeometry : wkts) {
            criterion.add(GeometryCriterionBuilder.build(wktGeometry));
        }
        return criterion.isEmpty() ? ICriterion.all() : ICriterion.and(criterion);
    }

    /**
     * Build a {@link ICriterion} to search for the lists of geometry bbox given.
     *
     * @param bboxs {@link String}s
     * @return {@link ICriterion}
     */
    public static ICriterion buildGeometryBbox(List<String> bboxs) throws InvalidGeometryException {
        List<ICriterion> criterion = new ArrayList<>();
        for (String bbox : bboxs) {
            criterion.add(GeometryCriterionBuilder.buildBbox(bbox));
        }
        return criterion.isEmpty() ? ICriterion.all() : ICriterion.and(criterion);
    }

    /**
     * Build a {@link ICriterion} to search for the lists of geometry circle given. A circle is a longitude, latitude
     * and radius.
     *
     * @param longitude {@link String}
     * @param latitude  {@link String}
     * @param radius    {@link String}
     * @return {@link ICriterion}
     */
    public static ICriterion buildGeometryCircle(List<String> longitude, List<String> latitude, List<String> radius)
        throws InvalidGeometryException {
        List<ICriterion> criterion = new ArrayList<>();
        if ((longitude.size() == latitude.size()) && (latitude.size() == radius.size())) {
            for (int i = 0; i < longitude.size(); i++) {
                criterion.add(GeometryCriterionBuilder.build(longitude.get(i), latitude.get(i), radius.get(i)));
            }
        } else {
            throw new InvalidGeometryException("Missing one of latitude, longitude or radius to create circle geometry");
        }
        return criterion.isEmpty() ? ICriterion.all() : ICriterion.and(criterion);
    }
}
