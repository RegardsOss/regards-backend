/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.dam.domain.entities.criterion;

import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Search criterion builder from an {@link AttributeModel}.<br/>
 * This builder places the right data JSON path according to the related attribute model.
 *
 * @author Marc Sordi
 */
public interface IFeatureCriterion extends ICriterion {

    Logger LOGGER = LoggerFactory.getLogger(IFeatureCriterion.class);

    /**
     * Separator to detect and parse string matching behavior
     */
    String STRING_MATCH_TYPE_SEPARATOR = "@";

    static <T extends Number & Comparable<T>> ICriterion gt(AttributeModel att, T value) {
        return ICriterion.gt(att.getFullJsonPath(), value);
    }

    static <T extends Number & Comparable<T>> ICriterion ge(AttributeModel att, T value) {
        return ICriterion.ge(att.getFullJsonPath(), value);
    }

    static <T extends Number & Comparable<T>> ICriterion lt(AttributeModel att, T value) {
        return ICriterion.lt(att.getFullJsonPath(), value);
    }

    static <T extends Number & Comparable<T>> ICriterion le(AttributeModel att, T value) {
        return ICriterion.le(att.getFullJsonPath(), value);
    }

    static ICriterion gt(AttributeModel att, OffsetDateTime date) {
        return ICriterion.gt(att.getFullJsonPath(), date);
    }

    static ICriterion ge(AttributeModel att, OffsetDateTime date) {
        return ICriterion.ge(att.getFullJsonPath(), date);
    }

    static ICriterion lt(AttributeModel att, OffsetDateTime date) {
        return ICriterion.lt(att.getFullJsonPath(), date);
    }

    static ICriterion le(AttributeModel att, OffsetDateTime date) {
        return ICriterion.le(att.getFullJsonPath(), date);
    }

    static ICriterion eq(AttributeModel att, int value) {
        return ICriterion.eq(att.getFullJsonPath(), value);
    }

    static ICriterion eq(AttributeModel att, long value) {
        return ICriterion.eq(att.getFullJsonPath(), value);
    }

    static ICriterion isTrue(AttributeModel att) {
        return ICriterion.isTrue(att.getFullJsonPath());
    }

    static ICriterion isFalse(AttributeModel att) {
        return ICriterion.isFalse(att.getFullJsonPath());
    }

    static ICriterion eq(AttributeModel att, boolean value) {
        return ICriterion.eq(att.getFullJsonPath(), value);
    }

    static ICriterion in(AttributeModel att, int... values) {
        return ICriterion.in(att.getFullJsonPath(), values);
    }

    static ICriterion in(AttributeModel att, long... values) {
        return ICriterion.in(att.getFullJsonPath(), values);
    }

    static ICriterion in(AttributeModel att, double[] values, double precision) {
        return ICriterion.in(att.getFullJsonPath(), values, precision);
    }

    static ICriterion eq(AttributeModel att, double value, double precision) {
        return ICriterion.eq(att.getFullJsonPath(), value, precision);
    }

    static ICriterion ne(AttributeModel att, int value) {
        return ICriterion.ne(att.getFullJsonPath(), value);
    }

    static ICriterion ne(AttributeModel att, long value) {
        return ICriterion.ne(att.getFullJsonPath(), value);
    }

    static ICriterion ne(AttributeModel att, double value, double precision) {
        return ICriterion.ne(att.getFullJsonPath(), value, precision);
    }

    /**
     * Criterion to test if a parameter is exactly the provided text or if a String array parameter contains an element
     * which is exactly the provided text
     *
     * @param att       {@link AttributeModel}
     * @param text      provided text
     * @param matchType string matching behavior
     * @return criterion
     */
    static ICriterion eq(AttributeModel att, String text, StringMatchType matchType) {
        return ICriterion.eq(att.getFullJsonPath(), text, matchType);
    }

    /**
     * Criterion to test if a parameter is exactly the provided date
     *
     * @param att  {@link AttributeModel}
     * @param date provided text
     * @return criterion
     */
    static ICriterion eq(AttributeModel att, OffsetDateTime date) {
        return ICriterion.eq(att.getFullJsonPath(), date);
    }

    /**
     * Criterion to test if a parameter starts with the provided text or if a String array parameter contains an element
     * that starts with the provided text
     *
     * @param att       {@link AttributeModel}
     * @param text      provided text
     * @param matchType string matching behavior
     * @return criterion
     */
    static ICriterion startsWith(AttributeModel att, String text, StringMatchType matchType) {
        return ICriterion.startsWith(att.getFullJsonPath(), text, matchType);
    }

    /**
     * Criterion to test if a parameter ends with the provided text or if a String array parameter contains an element
     * that ends with the provided text
     *
     * @param att       {@link AttributeModel}
     * @param text      provided text
     * @param matchType string matching behavior
     * @return criterion
     */
    static ICriterion endsWith(AttributeModel att, String text, StringMatchType matchType) {
        return ICriterion.endsWith(att.getFullJsonPath(), text, matchType);
    }

    /**
     * Criterion to test if a parameter contain the provided text or if a String array parameter contains an element
     * that contains the provided text
     *
     * @param att       {@link AttributeModel}
     * @param text      provided text
     * @param matchType string matching behavior
     * @return criterion
     */
    static ICriterion contains(AttributeModel att, String text, StringMatchType matchType) {
        return ICriterion.contains(att.getFullJsonPath(), text, matchType);
    }

    /**
     * Criterion to test if a parameter follows given regular expression or if a String array parameter contains an
     * element which follows given regular expression
     *
     * @param att       {@link AttributeModel}
     * @param text      provided regular expression
     * @param matchType string matching behavior
     * @return criterion
     */
    static ICriterion regexp(AttributeModel att, String text, StringMatchType matchType) {
        return ICriterion.regexp(att.getFullJsonPath(), text, matchType);
    }

    /**
     * Criterion to test if an array parameter contains specified value
     *
     * @param att   {@link AttributeModel}
     * @param value value to search
     * @return criterion
     */
    static ICriterion contains(AttributeModel att, int value) {
        return ICriterion.contains(att.getFullJsonPath(), value);
    }

    /**
     * Criterion to test if an array parameter contains specified value
     *
     * @param att   {@link AttributeModel}
     * @param value value to search
     * @return criterion
     */
    static ICriterion contains(AttributeModel att, long value) {
        return ICriterion.contains(att.getFullJsonPath(), value);
    }

    /**
     * Criterion to test if a double array parameter contains specified double value specifying precision
     *
     * @param att       {@link AttributeModel}
     * @param value     value to search
     * @param precision wanted precision
     * @return criterion
     */
    static ICriterion contains(AttributeModel att, double value, double precision) {
        return ICriterion.contains(att.getFullJsonPath(), value, precision);
    }

    /**
     * Criterion to test if a date array parameter contains a date between given lower and upper dates
     *
     * @param att       {@link AttributeModel}
     * @param lowerDate inclusive lower bound
     * @param upperDate inclusive upper bound
     * @return criterion
     */
    static ICriterion containsDateBetween(AttributeModel att, OffsetDateTime lowerDate, OffsetDateTime upperDate) {
        return ICriterion.containsDateBetween(att.getFullJsonPath(), lowerDate, upperDate);
    }

    /**
     * Criterion to test if a string parameter has one of the provided values
     *
     * @param att       {@link AttributeModel}
     * @param matchType string matching behavior
     * @param texts     text array to test
     * @return criterion
     */
    static ICriterion in(AttributeModel att, StringMatchType matchType, String... texts) {
        return ICriterion.in(att.getFullJsonPath(), matchType, texts);
    }

    /**
     * Criterion to test if an int parameter has a value into given range
     *
     * @param att   {@link AttributeModel}
     * @param lower inclusive lower bound
     * @param upper inclusive upper bound
     * @return criterion
     */
    static ICriterion between(AttributeModel att, int lower, int upper) {
        return ICriterion.between(att.getFullJsonPath(), lower, upper);
    }

    /**
     * Criterion to test if a long parameter has a value into given range
     *
     * @param att   {@link AttributeModel}
     * @param lower inclusive lower bound
     * @param upper inclusive upper bound
     * @return criterion
     */
    static ICriterion between(AttributeModel att, long lower, long upper) {
        return ICriterion.between(att.getFullJsonPath(), lower, upper);
    }

    /**
     * Criterion to test if a date parameter is into given range period
     *
     * @param att   {@link AttributeModel}
     * @param lower inclusive lower bound
     * @param upper inclusive upper bound
     * @return criterion
     */
    static ICriterion between(AttributeModel att, OffsetDateTime lower, OffsetDateTime upper) {
        return ICriterion.between(att.getFullJsonPath(), lower, upper);
    }

    /**
     * Criterion to test if a double parameter has a value into given range
     *
     * @param att   {@link AttributeModel}
     * @param lower inclusive lower bound
     * @param upper inclusive upper bound
     * @return criterion
     */
    static ICriterion between(AttributeModel att, double lower, double upper) {
        return ICriterion.between(att.getFullJsonPath(), lower, upper);
    }

    /**
     * Criterion to test if an int parameter has a value into given range
     *
     * @param att            {@link AttributeModel}
     * @param lower          lower bound
     * @param lowerInclusive inclusive lower bound or not
     * @param upper          upper bound
     * @param upperInclusive inclusive upper bound or not
     * @return criterion
     */
    static ICriterion between(AttributeModel att,
                              int lower,
                              boolean lowerInclusive,
                              int upper,
                              boolean upperInclusive) {
        return ICriterion.between(att.getFullJsonPath(), lower, lowerInclusive, upper, upperInclusive);
    }

    /**
     * Criterion to test if a long parameter has a value into given range
     *
     * @param att            {@link AttributeModel}
     * @param lower          inclusive lower bound
     * @param lowerInclusive inclusive lower bound or not
     * @param upper          inclusive upper bound
     * @param upperInclusive inclusive upper bound or not
     * @return criterion
     */
    static ICriterion between(AttributeModel att,
                              long lower,
                              boolean lowerInclusive,
                              long upper,
                              boolean upperInclusive) {
        return ICriterion.between(att.getFullJsonPath(), lower, lowerInclusive, upper, upperInclusive);
    }

    /**
     * Criterion to test if a date parameter is into given range period
     *
     * @param att            {@link AttributeModel}
     * @param lower          inclusive lower bound
     * @param lowerInclusive inclusive lower bound or not
     * @param upper          inclusive upper bound
     * @param upperInclusive inclusive upper bound or not
     * @return criterion
     */
    static ICriterion between(AttributeModel att,
                              OffsetDateTime lower,
                              boolean lowerInclusive,
                              OffsetDateTime upper,
                              boolean upperInclusive) {
        return ICriterion.between(att.getFullJsonPath(), lower, lowerInclusive, upper, upperInclusive);
    }

    /**
     * Criterion to test if a double parameter has a value into given range
     *
     * @param att            {@link AttributeModel}
     * @param lower          inclusive lower bound
     * @param lowerInclusive inclusive lower bound or not
     * @param upper          inclusive upper bound
     * @param upperInclusive inclusive upper bound or not
     * @return criterion
     */
    static ICriterion between(AttributeModel att,
                              double lower,
                              boolean lowerInclusive,
                              double upper,
                              boolean upperInclusive) {
        return ICriterion.between(att.getFullJsonPath(), lower, lowerInclusive, upper, upperInclusive);
    }

    /**
     * Criterion to test if a numeric value (int or double) is into (inclusive) given interval attribute name
     *
     * @param <T>   extends {@link Number}
     * @param att   {@link AttributeModel}
     * @param value value to test inclusion
     * @return criterion
     */
    static <T extends Number & Comparable<T>> ICriterion into(AttributeModel att, T value) {
        return ICriterion.into(att.getFullJsonPath(), value);
    }

    /**
     * Criterion to test if given date range intersects given interval attribute name
     *
     * @param att        {@link AttributeModel}
     * @param lowerBound lower bound
     * @param upperBound upper bound
     * @return criterion
     */
    static ICriterion intersects(AttributeModel att, OffsetDateTime lowerBound, OffsetDateTime upperBound) {
        return ICriterion.intersects(att.getFullJsonPath(), lowerBound, upperBound);
    }

    /**
     * Criterion to test if given number range intersects given interval attribute name
     *
     * @param <T>        extends {@link Number}
     * @param att        {@link AttributeModel}
     * @param lowerBound lower bound
     * @param upperBound upper bound
     * @return criterion
     */
    static <T extends Number & Comparable<T>> ICriterion intersects(AttributeModel att, T lowerBound, T upperBound) {
        return ICriterion.intersects(att.getFullJsonPath(), lowerBound, upperBound);
    }

    /**
     * Criterion to test if given attribute exists
     *
     * @param att {@link AttributeModel}
     * @return criterion
     */
    static ICriterion attributeExists(AttributeModel att) {
        return ICriterion.attributeExists(att.getFullJsonPath());
    }

    /**
     * Criterion to test if at least one of the parameters contains the provided text
     *
     * @param atts list of attributes
     * @param text provided regular expression
     * @return criterion
     */
    static ICriterion multiMatch(Set<AttributeModel> atts, String text) {
        Set<String> attNames = new HashSet<>();
        atts.forEach(att -> attNames.add(att.getFullJsonPath()));
        return ICriterion.multiMatch(attNames, text);
    }

    /**
     * Criterion to test if at least one of the parameters starts with the provided text
     *
     * @param atts list of attributes
     * @param text provided regular expression
     * @return criterion
     */
    static ICriterion multiMatchStartWith(Set<AttributeModel> atts, String text) {
        Set<String> attNames = new HashSet<>();
        atts.forEach(att -> attNames.add(att.getFullJsonPath()));
        return ICriterion.multiMatchStartWith(attNames, text);
    }

    /**
     * Method to parse string field ant its matching behavior when search field is build as follow :
     * {field_name}+{@link #STRING_MATCH_TYPE_SEPARATOR}+{matchTypeValue}
     * <br/>
     * <br/>
     * <p>
     * <p>
     * If field contains {@link #STRING_MATCH_TYPE_SEPARATOR},
     * retrieve real field name and extract string matching behavior.<br/>
     * If not, just return field parameter as is!<br/>
     * Parsing must be done before finding related attribute to get the real attribute name to find.
     *
     * @param field field to parse
     * @return Pair representing real field name and its string matching behavior (only representative for strings)
     */
    static Pair<String, StringMatchType> parse(String field) {
        if (field.contains(STRING_MATCH_TYPE_SEPARATOR)) {
            String[] fieldParts = field.split(STRING_MATCH_TYPE_SEPARATOR);
            StringMatchType matchType;

            Optional<StringMatchType> stringMatchType = parseStringMatchType(fieldParts[1]);
            if (stringMatchType.isPresent()) {
                matchType = stringMatchType.get();
            } else {
                // Default behavior
                matchType = StringMatchType.KEYWORD;
                LOGGER.warn("Cannot detect string matching behavior with field {} and behavior {}. Falling back to {}!",
                            field,
                            fieldParts[1],
                            matchType);
            }
            return Pair.of(fieldParts[0], matchType);
        }
        // Default behavior
        return Pair.of(field, StringMatchType.KEYWORD);
    }

    static Optional<StringMatchType> parseStringMatchType(String matchTypeValue) {
        return Arrays.stream(StringMatchType.values())
                     .filter(t -> t.getMatchTypeValue().equalsIgnoreCase(matchTypeValue))
                     .findFirst();
    }
}
