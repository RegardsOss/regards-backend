/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
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
@Event(target = Target.MICROSERVICE)
public class NewProjectConnectionEvent implements ISubscribable {

    /**
     * The new project to manage
     */
    private ProjectConnection newProjectConnection;

    public NewProjectConnectionEvent() {
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
    public NewProjectConnectionEvent(final ProjectConnection pNewProjectConnection) {
        super();
        newProjectConnection = pNewProjectConnection;
    }

    public ProjectConnection getNewProjectConnection() {
        return newProjectConnection;
    }

    public void setNewProjectConnection(final ProjectConnection pNewProjectConnection) {
        newProjectConnection = pNewProjectConnection;
    }

}
