/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.event;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.IPoller;

/**
 * @author Marc Sordi
 *
 */
@Component
public class PollableServiceBean extends PollableService<PollOneAllEvent> {

    public PollableServiceBean(IPoller pPoller) {
        super(pPoller);
    }

}
