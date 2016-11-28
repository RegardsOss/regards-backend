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

    /**
     * Authentication is successful, access is granted
     */
    ACCESS_GRANTED,
    /**
     * Authentication error, access denied
     */
    ACCESS_DENIED,

    /**
     * Authentication error, invalid password
     */
    INVALID_PASSWORD,
    /**
     * Authentication error, password expired
     */
    PASSWORD_EXPIRED,

    /**
     * Authentication error, account does not exists
     */
    ACCOUNT_NOT_FOUND;

    @Override
    public String toString() {
        return this.name();
    }

}
