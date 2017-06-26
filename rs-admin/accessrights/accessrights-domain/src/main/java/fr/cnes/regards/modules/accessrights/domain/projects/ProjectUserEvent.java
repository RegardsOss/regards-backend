package fr.cnes.regards.modules.accessrights.domain.projects;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Event(target = Target.ALL)
public class ProjectUserEvent implements ISubscribable {

    private String email;

    private ProjectUserAction action;

    public ProjectUserEvent() {}

    public ProjectUserEvent(String email, ProjectUserAction action) {
        this.email = email;
        this.action = action;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ProjectUserAction getAction() {
        return action;
    }

    public void setAction(ProjectUserAction action) {
        this.action = action;
    }
}
