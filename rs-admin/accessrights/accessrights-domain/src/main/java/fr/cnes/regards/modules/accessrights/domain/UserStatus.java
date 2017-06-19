/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Enumerates possible values for {@link ProjectUser#status}
 *
 * @author Xavier-Alexandre Brochard
 */
public enum UserStatus {

    WAITING_ACCESS,
    ACCESS_DENIED,
    ACCESS_GRANTED,
    ACCESS_INACTIVE,
    WAITING_ACCOUNT_ACTIVE,
    WAITING_EMAIL_VERIFICATION;

    @Override
    public String toString() {
        return this.name();
    }
}
