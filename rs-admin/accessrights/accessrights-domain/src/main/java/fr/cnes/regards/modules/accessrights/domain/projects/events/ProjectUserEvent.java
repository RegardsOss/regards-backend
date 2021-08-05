package fr.cnes.regards.modules.accessrights.domain.projects.events;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Event(target = Target.ALL)
public class ProjectUserEvent implements ISubscribable {

    /**
     * The project user email
     */
    private String email;

    /**
     * The action
     */
    private ProjectUserAction action;

    /**
     * Default constructor
     */
    public ProjectUserEvent() {}

    /**
     * Constructor setting the parameters as attributes
     * @param email
     * @param action
     */
    public ProjectUserEvent(String email, ProjectUserAction action) {
        this.email = email;
        this.action = action;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set the email
     * @param email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the action
     */
    public ProjectUserAction getAction() {
        return action;
    }

    /**
     * Set the action
     * @param action
     */
    public void setAction(ProjectUserAction action) {
        this.action = action;
    }
}
