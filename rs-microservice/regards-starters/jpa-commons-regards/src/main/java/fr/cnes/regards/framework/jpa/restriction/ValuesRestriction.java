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
package fr.cnes.regards.framework.jpa.restriction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collection;

/**
 * Restriction used in specification builder.
 * Determine if a given field value is included or excluded from a collection of values.
 *
 * @author Théo Lasserre
 */
public class ValuesRestriction<T> {

    @NotNull
    @Size(max = 1000, message = "List of values have a maximal size of 1000")
    @Schema(description = "List of accepted or refused values (according to the mode)")
    private Collection<T> values;

    @NotNull
    @Schema(description = "Restriction mode to use")
    private ValuesRestrictionMode mode;

    @Schema(description = "Match mode to use, can be STRICT|STARTS_WITH|CONTAINS|ENDS_WITH (default STRICT)")
    private ValuesRestrictionMatchMode matchMode = ValuesRestrictionMatchMode.STRICT;

    @Schema(description = "Only available for String resitrctions. Allow if true, to search values ignoring case. "
                          + "Default false")
    private boolean ignoreCase = false;

    public ValuesRestriction() {
    }

    public ValuesRestriction(Collection<T> values, ValuesRestrictionMode mode) {
        this.values = values;
        this.mode = mode;
    }

    public ValuesRestriction(Collection<T> values,
                             ValuesRestrictionMode mode,
                             ValuesRestrictionMatchMode matchMode,
                             boolean ignoreCase) {
        this.values = values;
        this.mode = mode;
        this.matchMode = matchMode;
        this.ignoreCase = ignoreCase;
    }

    public ValuesRestriction<T> withInclude(Collection<T> values) {
        this.values = values;
        this.mode = ValuesRestrictionMode.INCLUDE;
        return this;
    }

    public ValuesRestrictionMatchMode getMatchMode() {
        return matchMode;
    }

    public ValuesRestriction<T> withMatchMode(ValuesRestrictionMatchMode matchMode) {
        this.matchMode = matchMode;
        return this;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public ValuesRestriction<T> withIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
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

    @Override
    public String toString() {
        return "ValuesRestriction{"
               + "ignoreCase="
               + ignoreCase
               + ", values="
               + values
               + ", mode="
               + mode
               + ", matchMode="
               + matchMode
               + '}';
    }
}
