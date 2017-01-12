/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.event;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationMode;
import fr.cnes.regards.framework.amqp.domain.AmqpCommunicationTarget;

/**
 *
 * Utility class to extract annotation information from events.
 *
 * @author Marc Sordi
 *
 */
public final class EventUtils {

    private EventUtils() {
    }

    /**
     * Retrieve annotation {@link EventProperties} from class. This annotation must exist!
     *
     * @param pClass
     *            {@link EventProperties} annotated class
     * @return {@link EventProperties}
     */
    public static EventProperties getEventProperties(Class<?> pClass) {
        Assert.notNull(pClass);
        EventProperties ppt = AnnotationUtils.findAnnotation(pClass, EventProperties.class);
        Assert.notNull(ppt);
        return ppt;
    }

    /**
     *
     * @param pClass
     *            {@link EventProperties} annotated class
     * @return {@link AmqpCommunicationMode}
     */
    public static AmqpCommunicationMode getCommunicationMode(Class<?> pClass) {
        EventProperties ppt = EventUtils.getEventProperties(pClass);

        AmqpCommunicationMode mode;
        if (WorkerMode.SINGLE.equals(ppt.mode())) {
            mode = AmqpCommunicationMode.ONE_TO_ONE;
        } else
            if (WorkerMode.ALL.equals(ppt.mode())) {
                mode = AmqpCommunicationMode.ONE_TO_MANY;
            } else {
                throw new IllegalArgumentException();
            }
        return mode;
    }

    /**
     *
     * @param pClass
     *            {@link EventProperties} annotated class
     * @return {@link AmqpCommunicationMode}
     */
    public static AmqpCommunicationTarget getCommunicationTarget(Class<?> pClass) {
        EventProperties ppt = EventUtils.getEventProperties(pClass);
        AmqpCommunicationTarget target;
        if (Target.ALL.equals(ppt.target())) {
            target = AmqpCommunicationTarget.ALL;
        } else
            if (Target.MICROSERVICE.equals(ppt.target())) {
                target = AmqpCommunicationTarget.MICROSERVICE;
            } else {
                throw new IllegalArgumentException();
            }
        return target;
    }

}
