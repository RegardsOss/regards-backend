/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.jpa.utils;

import org.hibernate.dialect.PostgreSQL9Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

/**
 * Current dialect in use in REGARDS
 * This class add some custom Postgres functions and expressions you can use in Specification classes,
 * allowing us to query the database using custom SQL, mostly to use JSONB operators
 *
 * Keep in mind that every operators containing question mark cannot be written here
 * as JDBC would interpret the question mark as a prepared value not provided,
 * so we've created some pre-defined function to make an alias to the operator, in order to fix this issue
 *
 * You can find the function name of an operator with the following SQL :
 * SELECT oprname, oprcode  FROM pg_operator WHERE oprname = '&!'
 *
 * @author Léo Mieulet
 */
public class CustomPostgresDialect extends PostgreSQL9Dialect {

    /**
     * Create an new empty array
     */
    public static String EMPTY_STRING_ARRAY = "new_string_array";

    /**
     * Create a literal casted as string
     */
    public static String STRING_LITERAL = "string_literal";

    /**
     * Create a literal casted as jsonb
     */
    public static String JSONB_LITERAL = "jsonb_literal";

    /**
     * Create an expression with the @> operator
     */
    public static String JSONB_CONTAINS = "jsonb_contains";

    /**
     * Alias to the operator ?&; ensure indexes are used if possible
     */
    public static String JSONB_EXISTS_ALL = "rs_jsonb_exists_all";

    /**
     * Alias to the operator ?; ensure indexes are used if possible
     */
    public static String JSONB_EXISTS = "rs_jsonb_exists";

    /**
     * Alias to the operator ?|; ensure indexes are used if possible
     */
    public static String JSONB_EXISTS_ANY = "rs_jsonb_exists_any";

    public CustomPostgresDialect() {
        super();
        registerFunction(EMPTY_STRING_ARRAY, new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "ARRAY[]::text[]"));
        registerFunction(STRING_LITERAL, new SQLFunctionTemplate(StandardBasicTypes.STRING, "?1::text"));
        registerFunction(JSONB_LITERAL, new SQLFunctionTemplate(StandardBasicTypes.STRING, "?1::jsonb"));
        registerFunction(JSONB_CONTAINS, new SQLFunctionTemplate(StandardBasicTypes.BOOLEAN, "?1 @> ?2"));
    }
}
