/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.internal;

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
     * Login error login or password invalid.
     */
    ACCOUNT_UNKNOWN,

    /**
     * Account is not validated by administrator yet
     */
    ACCOUNT_PENDING,

    /**
     * Account inactive
     */
    ACCOUNT_INACTIVE,

    /**
     * Password locked.
     */
    ACCOUNT_LOCKED,

    /**
     * Account does not have a user for the request project
     */
    USER_UNKNOWN,

    /**
     * Waiting for the account to be active
     */
    USER_WAITING_ACCOUNT_ACTIVE,

    /**
     * User access to project is not validated yet
     */
    USER_WAITING_ACCESS,

    /**
     * Waiting for the user to click on the link in the verification email
     */
    USER_WAITING_EMAIL_VERIFICATION,

    /**
     * User access denied.
     */
    USER_ACCESS_DENIED,

    /**
     * User access inactive
     */
    USER_ACCESS_INACTIVE,

    /**
     * User cannot access instance tenant
     */
    INSTANCE_ACCESS_DENIED;

    @Override
    public String toString() {
        return this.name();
    }

}
