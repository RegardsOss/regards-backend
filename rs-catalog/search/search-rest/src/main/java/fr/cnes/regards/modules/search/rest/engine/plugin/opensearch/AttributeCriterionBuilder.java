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
package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch;

import java.util.List;

import fr.cnes.regards.framework.gson.adapters.OffsetDateTimeAdapter;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.opensearch.service.exception.OpenSearchUnknownParameter;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.exception.UnsupportedCriterionOperator;

/**
 * {@link ICriterion} builder for catalog searches.
 * @author Sébastien Binda
 */
public class AttributeCriterionBuilder {

    private AttributeCriterionBuilder() {
    }

    /**
     * Build a {@link ICriterion} for catalog search from
     * @param attributeName {@link AttributeModel#getName()}
     * @param operator {@link ParameterOperator} to apply for the current attribute search
     * @param values {@link String}s search values.
     * @return {@link ICriterion}
     * @throws UnsupportedCriterionOperator
     */
    public static ICriterion build(AttributeModel attributeModel, ParameterOperator operator, List<String> values)
            throws UnsupportedCriterionOperator {
        ICriterion criterion = null;
        String attributeJsonPath = attributeModel.getJsonPath();
        for (String value : values) {
            ICriterion valueCriterion = null;
            switch (attributeModel.getType()) {
                case INTEGER:
                case INTEGER_ARRAY:
                    valueCriterion = buildIngetegerCrit(attributeJsonPath, value, operator);
                    break;
                case DOUBLE:
                case DOUBLE_ARRAY:
                    valueCriterion = buildDoubleCrit(attributeJsonPath, value, operator);
                    break;
                case LONG:
                case LONG_ARRAY:
                    valueCriterion = buildLongCrit(attributeJsonPath, value, operator);
                    break;
                case STRING:
                case URL:
                    valueCriterion = buildStringCrit(attributeJsonPath, value, operator);
                    break;
                case STRING_ARRAY:
                    valueCriterion = buildStringArrayCrit(attributeJsonPath, value, operator);
                    break;
                case DATE_ISO8601:
                    valueCriterion = buildDateCrit(attributeJsonPath, value, operator);
                    break;
                case BOOLEAN:
                    valueCriterion = buildBooleanCrit(attributeJsonPath, value, operator);
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
     * Build an Boolean {@link ICriterion} for catalog searches from a given {@link openSearchParameter} and a search value.
     * @param attribute {@link AttributeModel} to search for
     * @param value String representation of boolean parameter value
     * @param operator {@link ParameterOperator} for search
     * @return {@link ICriterion}
     * @throws UnsupportedCriterionOperator
     */
    private static ICriterion buildBooleanCrit(String attribute, String value, ParameterOperator operator)
            throws UnsupportedCriterionOperator {
        switch (operator) {
            case GE:
            case GT:
            case LE:
            case LT:
                throw new UnsupportedCriterionOperator(
                        String.format("Invalid operator %s for string parameter %s", operator.toString(), attribute));
            case EQ:
            default:
                return ICriterion.eq(attribute, Boolean.valueOf(value));
        }
    }

    /**
     * Build an Date {@link ICriterion} for catalog searches from a given {@link openSearchParameter} and a search value.
     * @param attribute {@link AttributeModel} to search for
     * @param value String representation of date parameter value
     * @param operator {@link ParameterOperator} for search
     * @return {@link ICriterion}
     * @throws OpenSearchUnknownParameter
     */
    private static ICriterion buildDateCrit(String attribute, String value, ParameterOperator operator) {
        switch (operator) {
            case GE:
                return ICriterion.ge(attribute, OffsetDateTimeAdapter.parse(value));
            case GT:
                return ICriterion.gt(attribute, OffsetDateTimeAdapter.parse(value));
            case LE:
                return ICriterion.le(attribute, OffsetDateTimeAdapter.parse(value));
            case LT:
                return ICriterion.lt(attribute, OffsetDateTimeAdapter.parse(value));
            case EQ:
            default:
                return ICriterion.eq(attribute, OffsetDateTimeAdapter.parse(value));
        }
    }

    /**
     * Build an Integer {@link ICriterion} for catalog searches from a given {@link openSearchParameter} and a search value.
     * @param attribute {@link AttributeModel} to search for
     * @param value String representation of integer parameter value
     * @param operator {@link ParameterOperator} for search
     * @return {@link ICriterion}
     * @throws OpenSearchUnknownParameter
     */
    private static ICriterion buildIngetegerCrit(String attribute, String value, ParameterOperator operator) {
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
                return ICriterion.ge(attribute, val);
            case GT:
                return ICriterion.gt(attribute, val);
            case LE:
                return ICriterion.le(attribute, val);
            case LT:
                return ICriterion.lt(attribute, val);
            case EQ:
            default:
                return ICriterion.eq(attribute, val);
        }
    }

    /**
     * Build an Double {@link ICriterion} for catalog searches from a given {@link openSearchParameter} and a search value.
     * @param attribute {@link AttributeModel} to search for
     * @param value String representation of double parameter value
     * @param operator {@link ParameterOperator} for search
     * @return {@link ICriterion}
     */
    private static ICriterion buildDoubleCrit(String attribute, String value, ParameterOperator operator) {
        Double asDouble = Double.parseDouble(value);
        switch (operator) {
            case GE:
                return ICriterion.ge(attribute, asDouble);
            case GT:
                return ICriterion.gt(attribute, asDouble);
            case LE:
                return ICriterion.le(attribute, asDouble);
            case LT:
                return ICriterion.lt(attribute, asDouble);
            case EQ:
            default:
                return ICriterion.eq(attribute, asDouble, asDouble - Math.nextDown(asDouble));
        }
    }

    /**
     * Build an Long {@link ICriterion} for catalog searches from a given {@link openSearchParameter} and a search value.
     * @param attribute {@link AttributeModel} to search for
     * @param value String representation of long parameter value
     * @param operator {@link ParameterOperator} for search
     * @return {@link ICriterion}
     */
    private static ICriterion buildLongCrit(String attribute, String value, ParameterOperator operator) {
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
                return ICriterion.ge(attribute, valL);
            case GT:
                return ICriterion.gt(attribute, valL);
            case LE:
                return ICriterion.le(attribute, valL);
            case LT:
                return ICriterion.lt(attribute, valL);
            case EQ:
            default:
                return ICriterion.eq(attribute, valL);
        }
    }

    /**
     * Build an String {@link ICriterion} for catalog searches from a given {@link openSearchParameter} and a search value.
     * @param attribute {@link AttributeModel} to search for
     * @param value String representation of parameter value
     * @param operator {@link ParameterOperator} for search
     * @return {@link ICriterion}
     * @throws UnsupportedCriterionOperator
     */
    private static ICriterion buildStringCrit(String attribute, String value, ParameterOperator operator)
            throws UnsupportedCriterionOperator {
        switch (operator) {
            case GE:
            case GT:
            case LE:
            case LT:
                throw new UnsupportedCriterionOperator(
                        String.format("Invalid operator %s for string parameter %s", operator.toString(), attribute));
            case EQ:
            default:
                return ICriterion.eq(attribute, value);
        }
    }

    /**
     * Build a String {@link ICriterion} for catalog searches from a given {@link openSearchParameter} and a search value.
     * @param attribute {@link AttributeModel} to search for
     * @param value String representation of parameter value
     * @param operator {@link ParameterOperator} for search
     * @return {@link ICriterion}
     * @throws UnsupportedCriterionOperator
     */
    private static ICriterion buildStringArrayCrit(String attribute, String value, ParameterOperator operator)
            throws UnsupportedCriterionOperator {
        switch (operator) {
            case GE:
            case GT:
            case LE:
            case LT:
                throw new UnsupportedCriterionOperator(
                        String.format("Invalid operator %s for string parameter %s", operator.toString(), attribute));
            case EQ:
            default:
                return ICriterion.contains(attribute, value);
        }
    }
}
