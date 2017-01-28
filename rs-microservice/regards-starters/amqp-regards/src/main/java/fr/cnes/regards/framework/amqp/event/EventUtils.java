/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.event;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

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
     * Retrieve annotation {@link Event} from class. This annotation must exist!
     *
     * @param pClass
     *            {@link Event} annotated class
     * @return {@link Event}
     */
    public static Event getEventProperties(Class<?> pClass) {
        Assert.notNull(pClass);
        Event ppt = AnnotationUtils.findAnnotation(pClass, Event.class);
        Assert.notNull(ppt);
        return ppt;
    }

    /**
     *
     * @param pClass
     *            {@link Event} annotated class
     * @return {@link WorkerMode}
     */
    public static WorkerMode getCommunicationMode(Class<?> pClass) {
        return EventUtils.getEventProperties(pClass).mode();
    }

    /**
     *
     * @param pClass
     *            {@link Event} annotated class
     * @return {@link WorkerMode}
     */
    public static Target getCommunicationTarget(Class<?> pClass) {
        return EventUtils.getEventProperties(pClass).target();
    }

}
