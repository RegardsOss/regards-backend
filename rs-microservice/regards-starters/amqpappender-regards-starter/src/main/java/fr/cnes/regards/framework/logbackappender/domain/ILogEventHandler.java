/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.logbackappender.domain;

import java.util.List;

import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;

/**
 * An interface used to define an handler to process the {@link LogEvent}
 * 
 * @author Christophe Mertz
 *
 */
public interface ILogEventHandler extends IHandler<LogEvent> {

    /**
     * Get all the LogEvent that have been handled.
     * 
     * @return a List of {@link TenantWrapper}.
     */
    public List<TenantWrapper<LogEvent>> getMessages();

}
