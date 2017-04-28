/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 *
 * This event must be sent when one or more resource access configuration change so security cache needs to be
 * refreshed.
 *
 * @author Marc Sordi
 *
 */
@Event(target = Target.ALL)
public class ResourceAccessEvent implements ISubscribable {

    /**
     * Name of the concerned microservice
     */
    private String microservice;

    public String getMicroservice() {
        return microservice;
    }

    public void setMicroservice(String pMicroservice) {
        microservice = pMicroservice;
    }
}
