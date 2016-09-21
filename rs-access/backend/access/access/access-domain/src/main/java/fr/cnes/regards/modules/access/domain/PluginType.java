/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

/**
 *
 * @author cmertz
 *
 */
public enum PluginType {

    ADMIN, PORTAL, USER;
	
    public String toString() {
        return this.name();
    }
}
