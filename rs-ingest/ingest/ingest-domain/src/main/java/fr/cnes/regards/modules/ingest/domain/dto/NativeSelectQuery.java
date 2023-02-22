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
package fr.cnes.regards.modules.ingest.domain.dto;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestriction;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMatchMode;
import fr.cnes.regards.framework.jpa.restriction.ValuesRestrictionMode;
import fr.cnes.regards.framework.jpa.utils.AbstractSpecificationsBuilder;

import java.util.*;

/**
 * Holds info about a query to run (with named parameters)
 *
 * @author Léo Mieulet
 */
public class NativeSelectQuery {

    /**
     * SQL select part
     */
    private final String selectClause;

    /**
     * SQL where part
     */
    private final String fromClause;

    /**
     * Predicates
     */
    private final Set<String> predicates;

    /**
     * Named parameters
     */
    private final Map<String, String> params;

    /**
     * Date named parameters
     */
    private final Map<String, Date> dateParams;

    public NativeSelectQuery(String selectClause, String fromClause) {
        this.selectClause = selectClause;
        this.fromClause = fromClause;
        params = new HashMap<>();
        dateParams = new HashMap<>();
        predicates = new HashSet<>();
    }

    /**
     * @return the SQL to execute
     */
    public String getSQL() {
        StringBuilder request = new StringBuilder("SELECT ").append(selectClause).append(" FROM ").append(fromClause);

        if (!predicates.isEmpty()) {
            request.append(" WHERE ");
            Joiner.on(" AND ").appendTo(request, predicates);
        }
        return request.toString();
    }

    public void andPredicate(String predicate, String paramName, String paramValue) {
        params.put(paramName, paramValue);
        predicates.add(predicate);
    }

    public void andPredicate(String predicate, String paramName, Date date) {
        dateParams.put(paramName, date);
        predicates.add(predicate);
    }

    /**
     * Build predicates by adding include sql native query as predicateStart + (paramValues) + predicateStop.
     * Where paramValues are compiled with param name to query.
     */
    public void andListPredicate(String predicateStart,
                                 String predicateStop,
                                 String rootParamName,
                                 Collection<String> paramValues) {
        Set<String> preparedPredicates = Sets.newHashSet();
        int i = 0;
        for (String paramValue : paramValues) {
            String paramName = rootParamName + i;
            preparedPredicates.add(":" + paramName);
            this.params.put(paramName, paramValue);
            i = i + 1;
        }
        predicates.add(predicateStart + String.join(" , ", preparedPredicates) + predicateStop);
    }

    public void addOneOfString(String rootParamName, ValuesRestriction<String> values) {
        Set<String> internalPredicates = Sets.newHashSet();
        int i = 0;
        for (String paramValue : values.getValues()) {
            String likeValue = AbstractSpecificationsBuilder.getLikeStringExpression(values.getMatchMode(), paramValue);
            String paramName = rootParamName + i;
            String operator;
            if (values.getMatchMode() == ValuesRestrictionMatchMode.STRICT) {
                if (values.getMode() == ValuesRestrictionMode.INCLUDE) {
                    operator = "=";
                } else {
                    operator = "!=";
                }
            } else {
                if (values.getMode() == ValuesRestrictionMode.INCLUDE) {
                    operator = "like";
                } else {
                    operator = "not like";
                }
            }
            internalPredicates.add("(" + rootParamName + " " + operator + " :" + paramName + ")");
            this.params.put(paramName, likeValue);
            i = i + 1;
        }
        String oneOf = Joiner.on(" OR ").join(internalPredicates);
        predicates.add("(" + oneOf + ")");
    }

    /**
     * @return params to inject after the prepare statement
     */
    public Map<String, String> getParams() {
        return params;
    }

    public Map<String, Date> getDateParams() {
        return dateParams;
    }
}
