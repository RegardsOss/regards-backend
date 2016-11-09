/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain;

public enum AccountStatus {
    /**
     * Account is inactive
     */
    INACTIVE,
    /**
     * Account request is accepted
     */
    ACCEPTED,
    /**
     * Account is active
     */
    ACTIVE,
    /**
     * Account is locked
     */
    LOCKED,
    /**
     * Account request is pending
     */
    PENDING;

    @Override
    public String toString() {
        return name();
    }
}
