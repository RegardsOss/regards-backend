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
package fr.cnes.regards.framework.hateoas;

import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.LinkRelation;

/**
 * Link relations
 * @author msordi
 */
public final class LinkRels {

    /**
     * Self
     */
    public static final LinkRelation SELF = IanaLinkRelations.SELF;

    /**
     * First
     */
    public static final LinkRelation FIRST = IanaLinkRelations.FIRST;

    /**
     * Previous
     */
    public static final LinkRelation PREVIOUS = IanaLinkRelations.PREV;

    /**
     * Next
     */
    public static final LinkRelation NEXT = IanaLinkRelations.NEXT;

    /**
     * Next
     */
    public static final LinkRelation LAST = IanaLinkRelations.LAST;

    /**
     * Create
     */
    public static final LinkRelation CREATE = LinkRelation.of("create");

    /**
     * Update
     */
    public static final LinkRelation UPDATE = LinkRelation.of("update");

    /**
     * Delete
     */
    public static final LinkRelation DELETE = LinkRelation.of("delete");

    /**
     * List of elements
     */
    public static final LinkRelation LIST = LinkRelation.of("list");

    private LinkRels() {
    }
}
