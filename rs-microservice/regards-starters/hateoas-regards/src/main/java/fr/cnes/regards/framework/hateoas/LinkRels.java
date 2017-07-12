/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.hateoas.Link;

/**
 * Link relations
 *
 * @author msordi
 *
 */
public final class LinkRels {

    /**
     * Self
     */
    public static final String SELF = Link.REL_SELF;

    /**
     * First
     */
    public static final String FIRST = Link.REL_FIRST;

    /**
     * Previous
     */
    public static final String PREVIOUS = Link.REL_PREVIOUS;

    /**
     * Next
     */
    public static final String NEXT = Link.REL_NEXT;

    /**
     * Next
     */
    public static final String LAST = Link.REL_LAST;

    /**
     * Create
     */
    public static final String CREATE = "create";

    /**
     * Update
     */
    public static final String UPDATE = "update";

    /**
     * Delete
     */
    public static final String DELETE = "delete";

    /**
     * List of elements
     */
    public static final String LIST = "list";

    private LinkRels() {
    }
}
