/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.domain;

/**
 *
 * @author cmertz
 *
 */
public enum ModuleType {

    ADMIN, PORTAL, USER;
	
    @Override
    public String toString() {
        return this.name();
    }
    
}
