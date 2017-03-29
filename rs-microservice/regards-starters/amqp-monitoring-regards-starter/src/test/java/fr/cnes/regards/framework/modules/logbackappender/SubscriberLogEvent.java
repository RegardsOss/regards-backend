/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.logbackappender;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.logbackappender.domain.LogEvent;

/**
 * This class is used to store the {@link LogEvent} received by the subscriber.
 
 * 
 * @author Christophe Mertz
 *
 */
@Component
public class SubscriberLogEvent {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberLogEvent.class);

    private List<TenantWrapper<LogEvent>> wrappers = new ArrayList<>();

    public void addLogEvent(TenantWrapper<LogEvent> logEvent) {
        wrappers.add(logEvent);
    }

    public List<TenantWrapper<LogEvent>> getMessages() {
        return wrappers;
    }

    public void reset() {
        wrappers.clear();
    }
}
