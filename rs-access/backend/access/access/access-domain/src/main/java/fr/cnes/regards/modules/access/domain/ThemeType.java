/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

/**
 *
 * @author cmertz
 *
 */
public enum ThemeType {

    ADMIN, PORTAL, USER, ALL;
	
    public String toString() {
        return this.name();
    }
}
