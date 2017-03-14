/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain.project;

/**
 *
 * @author Christophe Mertz
 *
 */
public enum ThemeType {

    /**
     * A {@link Theme} for the Admin frontend
     */
    ADMIN,
    
    /**
     * A {@link Theme} for the Portal frontend
     */
    PORTAL,
    
    /**
     * A {@link Theme} for the User frontend
     */
    USER,
    
    /**
     * A {@link Theme} for all the frontend
     */
    ALL;
	
    @Override
    public String toString() {
        return this.name();
    }
}
