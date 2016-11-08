/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins.domain;

/**
 *
 * Enumeration for authentication plugins status.
 *
 * @author Sébastien Binda
 *
 */
public enum AuthenticationStatus {

    ACCESS_GRANTED, ACCESS_DENIED, INVALID_PASSWORD, PASSWORD_EXPIRED, ACCOUNT_NOT_FOUD;

    @Override
    public String toString() {
        return this.name();
    }

}
