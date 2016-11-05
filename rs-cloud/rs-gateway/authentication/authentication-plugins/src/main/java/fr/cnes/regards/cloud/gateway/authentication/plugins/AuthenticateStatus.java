/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins;

/**
 *
 * Enumeration for authentication plugins status.
 *
 * @author Sébastien Binda
 *
 */
public enum AuthenticateStatus {

    ACCESS_GRANTED, ACCESS_DENIED, INVALID_PASSWORD, PASSWORD_EXPIRED, ACCOUNT_NOT_FOUD;

    @Override
    public String toString() {
        return this.name();
    }

}
