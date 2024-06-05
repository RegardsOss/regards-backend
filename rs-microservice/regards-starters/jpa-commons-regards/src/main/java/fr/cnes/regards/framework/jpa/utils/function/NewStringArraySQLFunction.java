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
package fr.cnes.regards.framework.jpa.utils.function;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * Custom function permitting to create a new empty array (instead of using ARRAY[]::text[])
 *
 * @author Olivier Rousselot
 */
public class NewStringArraySQLFunction extends StandardSQLFunction {

    /**
     * Function name
     */
    public static final String NAME = "new_string_array";

    /**
     * Please use String[].class instead of List.class
     */
    private static final BasicTypeReference<String[]> RETURN_TYPE = new BasicTypeReference<>("string_array",
                                                                                             String[].class,
                                                                                             SqlTypes.ARRAY);

    public NewStringArraySQLFunction() {
        super(NAME, false, RETURN_TYPE);
    }

    @Override
    public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> arguments, SqlAstTranslator<?> translator) {
        if (!arguments.isEmpty()) {
            throw new IllegalArgumentException("Function '" + super.getName() + "' requires no argument");
        }
        sqlAppender.append("ARRAY[]::text[]");
    }
}
