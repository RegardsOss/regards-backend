/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

/**
 * An enum for the type of the plugin
 * 
 * @author Christophe Mertz
 *
 */
public enum PluginType {

    /**
     * Admin
     */
    ADMIN,

    /**
     * Portal
     */
    PORTAL,

    /**
     * User
     */
    USER;

    public String toString() {
        return this.name();
    }
}
