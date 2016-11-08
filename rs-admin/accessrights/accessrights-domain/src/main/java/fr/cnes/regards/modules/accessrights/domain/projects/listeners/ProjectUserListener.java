/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects.listeners;

import java.time.LocalDateTime;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Entity listener<br>
 * Allows to intercept the entity manager in the lifecycle of an entity.
 *
 * @see https://docs.jboss.org/hibernate/entitymanager/3.5/reference/en/html/listeners.html
 *
 * @author Xavier-Alexandre Brochard
 */
public class ProjectUserListener {

    /**
     * Automaticly set lastUpate before any database persistence. This is intended to be the only place where lastUpdate
     * is updated.
     *
     * @param pProjectUser
     *            The listened project user
     */
    @PreUpdate
    @PrePersist
    public void setLastUpdate(final ProjectUser pProjectUser) {
        pProjectUser.setLastUpdate(LocalDateTime.now());
    }
}
