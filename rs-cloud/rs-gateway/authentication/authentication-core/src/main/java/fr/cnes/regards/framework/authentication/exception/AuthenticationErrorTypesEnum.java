/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.exception;

/**
 *
 * Class AuthenticationErrorTypesEnum
 *
 * Know error type for the regards frontend
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public enum AuthenticationErrorTypesEnum {

    /**
     * Login error login or password invalid.
     */
    ACCOUNT_UNKNOWN,

    /**
     * Account is not validated by administrator yet
     */
    ACCOUNT_PENDING,

    /**
     * Account not confirmed by user.
     */
    ACCOUNT_ACCEPTED,

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
     * User access to project is not validated yet
     */
    USER_WAITING_ACCESS,

    /**
     * User access denied.
     */
    USER_ACCESS_DENIED,

    /**
     * User access inactive
     */
    USER_ACCESS_INACTIVE;

}
