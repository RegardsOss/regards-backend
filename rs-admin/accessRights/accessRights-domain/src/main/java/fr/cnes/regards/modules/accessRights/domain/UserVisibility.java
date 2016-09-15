/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.domain;

/*
 * LICENSE_PLACEHOLDER
 */
public enum UserVisibility {
    READABLE, WRITEABLE, HIDDEN;

    @Override
    public String toString() {
        return this.name();
    }
}
