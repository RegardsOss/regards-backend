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
package fr.cnes.regards.framework.jpa.restriction;

import java.util.Collection;

/**
 * Restriction used in specification builder.
 * Determine if a given field value is included or excluded from a collection of values.
 *
 * @author Théo Lasserre
 */
public class ValuesRestriction {

    private Collection<?> values;
    private ValuesRestrictionMode mode;

    public ValuesRestriction(Collection<?> values, ValuesRestrictionMode mode) {
        this.values = values;
        this.mode = mode;
    }

    public static ValuesRestriction buildInclude(Collection<?> values) {
        return new ValuesRestriction(values, ValuesRestrictionMode.INCLUDE);
    }

    public static ValuesRestriction buildExclude(Collection<?> values) {
        return new ValuesRestriction(values, ValuesRestrictionMode.EXCLUDE);
    }

    public Collection<?> getValues() {
        return values;
    }

    public void setValues(Collection<?> values) {
        this.values = values;
    }

    public ValuesRestrictionMode getMode() {
        return mode;
    }

    public void setMode(ValuesRestrictionMode mode) {
        this.mode = mode;
    }
}
