/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.domain;

public enum AccountStatus {
    INACTIVE, ACCEPTED, ACTIVE, LOCKED, PENDING;

    @Override
    public String toString() {
        return this.name();
    }
}
