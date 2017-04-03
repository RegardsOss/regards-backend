/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.logbackappender.domain;

import fr.cnes.regards.framework.amqp.domain.IHandler;

/**
 * An interface used to define an handler to process the {@link LogEvent}
 * 
 * @author Christophe Mertz
 *
 */
public interface ILogEventHandler extends IHandler<LogEvent> {

}
