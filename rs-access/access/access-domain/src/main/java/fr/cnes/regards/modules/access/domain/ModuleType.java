/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

/**
 *
 * @author Christophe Mertz
 *
 */
public enum ModuleType {

    /**
     * A {@link Module} for the Admin frontend
     */
    ADMIN,
    
    /**
     * A {@link Module} for the Portal frontend
     */
    PORTAL,

    /**
     * A {@link Module} for the User frontend
     */
    USER;
	
    @Override
    public String toString() {
        return this.name();
    }
    
}
