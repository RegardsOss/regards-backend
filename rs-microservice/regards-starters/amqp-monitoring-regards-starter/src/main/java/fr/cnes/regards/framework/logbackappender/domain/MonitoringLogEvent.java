/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.logbackappender.domain;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;

/**
 * This class is used to subscribe to the {@link LogEvent} send by the REGARDS microservice
 * 
 * @author Christophe Mertz
 *
 */
public class MonitoringLogEvent implements IMonitoringLogEvent {

    public MonitoringLogEvent(ISubscriber subscriber, IHandler<LogEvent> handler) {
        super();
        subscriber.subscribeTo(LogEvent.class, handler);
    }

}
