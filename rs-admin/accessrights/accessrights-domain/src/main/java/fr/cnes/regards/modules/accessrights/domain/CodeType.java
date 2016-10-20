/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain;

public enum CodeType {
    RESET, UNLOCK;

    @Override
    public String toString() {
        return this.name();
    }
}
