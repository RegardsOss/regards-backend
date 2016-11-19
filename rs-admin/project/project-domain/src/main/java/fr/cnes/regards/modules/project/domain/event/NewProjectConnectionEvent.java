/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.domain.event;

import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class NewProjectEvent
 *
 * AMQ Event to manage project creation.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class NewProjectConnectionEvent {

    /**
     * The new project to manage
     */
    private final ProjectConnection newProjectConnection;

    /**
     *
     * Constructor
     *
     * @param pNewProject
     *            the new project to manage
     * @since 1.0-SNAPSHOT
     */
    public NewProjectConnectionEvent(final ProjectConnection pNewProjectConnection) {
        super();
        newProjectConnection = pNewProjectConnection;
    }

    public ProjectConnection getNewProjectConnection() {
        return newProjectConnection;
    }

}
