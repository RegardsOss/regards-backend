/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.logbackappender.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;

/**
 * This class is used to subscribe to the {@link LogEvent} send by the REGARDS microservice
 * 
 * @author Christophe Mertz
 *
 */
public class MonitoringLogEvent implements IMonitoringLogEvent {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(MonitoringLogEvent.class);

    public MonitoringLogEvent(ISubscriber subscriber, IHandler<LogEvent> handler) {
        super();
        LOG.debug("Subscription to logEvent queue");
        subscriber.subscribeTo(LogEvent.class, handler);
    }

}
