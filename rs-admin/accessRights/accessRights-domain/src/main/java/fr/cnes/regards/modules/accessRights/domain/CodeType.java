/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.domain;

public enum CodeType {
    RESET, UNLOCK;

    @Override
    public String toString() {
        return this.name();
    }
}
