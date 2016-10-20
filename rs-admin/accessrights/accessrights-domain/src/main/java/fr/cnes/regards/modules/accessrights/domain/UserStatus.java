/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain;

/*
 * LICENSE_PLACEHOLDER
 */
public enum UserStatus {

    WAITING_ACCESS, ACCESS_DENIED, ACCESS_GRANTED, ACCESS_INACTIVE;

    @Override
    public String toString() {
        return this.name();
    }
}
