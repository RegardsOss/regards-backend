/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.projectuser.workflow.events;

import org.springframework.context.ApplicationEvent;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Use this event in order to react to when the admin denies access of a project user.
 *
 * @author Xavier-Alexandre Brochard
 */
public class OnInactiveEvent extends ApplicationEvent {

    /**
     * The project user
     */
    private final ProjectUser projectUser;

    /**
     * @param pProjectUser
     */
    public OnInactiveEvent(ProjectUser pProjectUser) {
        super(pProjectUser);
        projectUser = pProjectUser;
    }

    /**
     * @return the projectUser
     */
    public ProjectUser getProjectUser() {
        return projectUser;
    }

}