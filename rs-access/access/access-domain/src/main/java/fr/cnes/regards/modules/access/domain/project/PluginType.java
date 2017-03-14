/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain.project;

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

    @Override
    public String toString() {
        return this.name();
    }
}
