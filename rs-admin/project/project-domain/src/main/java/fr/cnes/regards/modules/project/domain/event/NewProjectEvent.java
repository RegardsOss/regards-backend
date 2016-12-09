/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.domain.event;

import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Class NewProjectEvent
 *
 * AMQ Event to manage project creation.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class NewProjectEvent {

    /**
     * The new project to manage
     */
    private Project newProject;

    public NewProjectEvent() {
        super();
    }

    /**
     *
     * Constructor
     *
     * @param pNewProject
     *            the new project to manage
     * @since 1.0-SNAPSHOT
     */
    public NewProjectEvent(final Project pNewProject) {
        super();
        newProject = pNewProject;
    }

    public Project getNewProject() {
        return newProject;
    }

    public void setNewProject(final Project pNewProject) {
        newProject = pNewProject;
    }

}
