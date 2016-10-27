/*
 * LICENSE_PLACEHOLDER
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
