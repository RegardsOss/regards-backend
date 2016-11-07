/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

/**
 *
 * @author Christophe Mertz
 *
 */
public enum NewsType {

    /**
     * A private {@link News}
     */
    PRIVATE,
    
    /**
     * A public {@link News}
     */
    PUBLIC;
	
    @Override
    public String toString() {
        return this.name();
    }
}
