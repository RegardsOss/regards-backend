/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain;

public enum CodeType {
    /**
     * Type of the code given to the user to know that he is asking to have his account password reseted
     */
    RESET,
    /**
     * Type of the code given to the user to know that he is asking to unlock his account
     */
    UNLOCK;

    @Override
    public String toString() {
        return name();
    }
}
