/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.multitransactional;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * @author Marc Sordi
 *
 */
@Event(target = Target.ALL)
public class TodoEvent implements IPollable {

    private String label;

    public String getLabel() {
        return label;
    }

    public void setLabel(String pLabel) {
        label = pLabel;
    }

}
