/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.workflow.projectuser;

/**
 * Enum listing the three possible decisions an admin can make on a WAITING_ACCESS project user
 *
 * @author Xavier-Alexandre Brochard
 * @since 1.1-SNAPSHOT
 */
public enum AccessQualification {
    /**
     * The project user must be deleted
     */
    REJECTED,

    /**
     * The project user is kept with status ACCESS_DENIED
     */
    DENIED,

    /**
     * The project user is accepted and passed to status ACCES_GRANTED
     */
    GRANTED;
}
