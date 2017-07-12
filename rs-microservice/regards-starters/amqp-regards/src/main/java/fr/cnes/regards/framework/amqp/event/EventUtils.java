/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
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
    public static Target getCommunicationTarget(Class<?> pClass) {
        return EventUtils.getEventProperties(pClass).target();
    }

}
