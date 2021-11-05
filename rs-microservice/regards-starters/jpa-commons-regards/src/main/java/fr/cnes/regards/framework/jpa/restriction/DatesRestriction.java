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

import java.time.OffsetDateTime;
import java.util.Collection;

/**
 * Restriction used in specification builder.
 * Determine if a given date field value is between after & before dates.
 *
 * @author Théo Lasserre
 */
public class DatesRestriction {

    private OffsetDateTime after;
    private OffsetDateTime before;

    public DatesRestriction () {

    }

    public DatesRestriction(OffsetDateTime before, OffsetDateTime after) {
        this.after = after;
        this.before = before;
    }

    public static DatesRestriction buildBefore(OffsetDateTime before) {
        return new DatesRestriction(before, null);
    }

    public static DatesRestriction buildAfter(OffsetDateTime after) {
        return new DatesRestriction(null, after);
    }

    public static DatesRestriction buildBeforeAndAfter(OffsetDateTime before, OffsetDateTime after) {
        return new DatesRestriction(before, after);
    }

    public OffsetDateTime getAfter() {
        return after;
    }

    public void setAfter(OffsetDateTime after) {
        this.after = after;
    }

    public OffsetDateTime getBefore() {
        return before;
    }

    public void setBefore(OffsetDateTime before) {
        this.before = before;
    }
}
