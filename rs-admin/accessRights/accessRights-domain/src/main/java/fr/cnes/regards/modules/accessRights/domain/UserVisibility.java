/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.domain;

public enum UserVisibility {
    READABLE, WRITEABLE, HIDDEN;

    @Override
    public String toString() {
        return this.name();
    }
}
