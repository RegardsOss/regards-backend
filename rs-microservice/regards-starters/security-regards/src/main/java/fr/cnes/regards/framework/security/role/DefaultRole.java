/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.role;

/**
 * Lists the default roles available.
 *
 * @author Xavier-Alexandre Brochard
 */
public enum DefaultRole {

    /**
     * Cross tenant role / useful for cross tenant endpoints
     */
    // FIXME do not accept a new role with this name
    INSTANCE_ADMIN,

    /**
     * No access
     */
    // FIXME do not accept a new role with this name
    NONE,

    /**
     * Tenant main administrator
     */
    PROJECT_ADMIN,

    /**
     * Tenant administrator
     */
    ADMIN,

    /**
     * Tenant registered user
     */
    REGISTERED_USER,

    /**
     * Public
     */
    PUBLIC;
}