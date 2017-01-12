/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * An event must be annotated with {@link EventProperties} to identify its
 * {@link fr.cnes.regards.framework.amqp.event.Target}.<br\>
 *
 * For {@link IPollableEvent}, the {@link WorkerMode} can be customized. Default to {@link WorkerMode#SINGLE}. This
 * property is not used for {@link ISubscribableEvent}.
 *
 * @author Marc Sordi
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventProperties {

    /**
     * @return event {@link fr.cnes.regards.framework.amqp.event.Target}
     */
    fr.cnes.regards.framework.amqp.event.Target target();

    /**
     *
     * @return worker mode. Only use for {@link IPollableEvent}
     */
    WorkerMode mode() default WorkerMode.SINGLE;
}
