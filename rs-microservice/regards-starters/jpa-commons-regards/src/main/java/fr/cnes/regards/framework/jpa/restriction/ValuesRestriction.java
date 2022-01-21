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
package fr.cnes.regards.framework.jpa.restriction;

import com.sun.istack.NotNull;

import javax.validation.Valid;
import java.util.Collection;

/**
 * Restriction used in specification builder.
 * Determine if a given field value is included or excluded from a collection of values.
 *
 * @author Théo Lasserre
 */
public class ValuesRestriction<T> {

    @Valid
    @NotNull
    private Collection<T> values;

    @Valid
    @NotNull
    private ValuesRestrictionMode mode;

    public ValuesRestriction() {
    }

    public ValuesRestriction(Collection<T> values, ValuesRestrictionMode mode) {
        this.values = values;
        this.mode = mode;
    }

    public ValuesRestriction<T> withInclude(Collection<T> values) {
        this.values = values;
        this.mode = ValuesRestrictionMode.INCLUDE;
        return this;
    }

    public ValuesRestriction<T> withExclude(Collection<T> values) {
        this.values = values;
        this.mode = ValuesRestrictionMode.EXCLUDE;
        return this;
    }

    public Collection<T> getValues() {
        return values;
    }

    public void setValues(Collection<T> values) {
        this.values = values;
    }

    public ValuesRestrictionMode getMode() {
        return mode;
    }

    public void setMode(ValuesRestrictionMode mode) {
        this.mode = mode;
    }
}
