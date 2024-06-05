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
package fr.cnes.regards.framework.jpa.utils.function.contributor;

import fr.cnes.regards.framework.jpa.utils.function.NewStringArraySQLFunction;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;

/**
 * Define several custom functions to be easily used with Criteria API. It also defines some native postgreSQL functions (a priori).
 * This class is registered through META-INF/services/org.hibernate.boot.model.FunctionContributor
 *
 * @author Olivier Rousselot
 */
public class CustomFunctionsContributor implements FunctionContributor {

    /**
     * Create a literal casted as string
     */
    public static final String STRING_LITERAL = "string_literal";

    /**
     * Create a literal casted as jsonb
     */
    public static final String JSONB_LITERAL = "jsonb_literal";

    /**
     * Create an expression with the @> operator
     */
    public static final String JSONB_CONTAINS = "jsonb_contains";

    /**
     * Alias to the operator ?&; ensure indexes are used if possible
     */
    public static final String JSONB_EXISTS_ALL = "rs_jsonb_exists_all";

    /**
     * Alias to the operator ?; ensure indexes are used if possible
     */
    public static final String JSONB_EXISTS = "rs_jsonb_exists";

    /**
     * Alias to the operator ?|; ensure indexes are used if possible
     */
    public static final String JSONB_EXISTS_ANY = "rs_jsonb_exists_any";

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        BasicTypeRegistry basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();

        SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
        // return type isn't a basic one so a specific function is needed
        functionRegistry.register(NewStringArraySQLFunction.NAME, new NewStringArraySQLFunction());
        functionRegistry.registerPattern(STRING_LITERAL,
                                         "?1::text",
                                         basicTypeRegistry.resolve(StandardBasicTypes.STRING));
        functionRegistry.registerPattern(JSONB_LITERAL,
                                         "?1::jsonb",
                                         basicTypeRegistry.resolve(StandardBasicTypes.STRING));
        functionRegistry.registerPattern(JSONB_CONTAINS,
                                         "?1 @> ?2",
                                         basicTypeRegistry.resolve(StandardBasicTypes.BOOLEAN));
    }
}
