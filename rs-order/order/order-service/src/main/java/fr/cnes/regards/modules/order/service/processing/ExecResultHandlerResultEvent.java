/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.processing.domain.events.PExecutionResultEvent;
import io.vavr.collection.List;
import org.springframework.context.ApplicationEvent;

/**
 * Used by tests to be notified that an execution result has been received and dealt with properly.
 *
 * @author Guillaume  Andrieu
 */
@SuppressWarnings("serial")
public class ExecResultHandlerResultEvent extends ApplicationEvent {

    private final PExecutionResultEvent resultEvent;

    private final List<OrderDataFile> updatedOrderDataFiles;

    public ExecResultHandlerResultEvent(PExecutionResultEvent resultEvent, List<OrderDataFile> updatedOrderDataFiles) {
        super(new Object());
        this.resultEvent = resultEvent;
        this.updatedOrderDataFiles = updatedOrderDataFiles;
    }

    public PExecutionResultEvent getResultEvent() {
        return resultEvent;
    }

    public List<OrderDataFile> getUpdatedOrderDataFiles() {
        return updatedOrderDataFiles;
    }

    public static ExecResultHandlerResultEvent event(PExecutionResultEvent resultEvent,
            java.util.Collection<OrderDataFile> updatedOrderDataFiles) {
        return new ExecResultHandlerResultEvent(resultEvent, List.ofAll(updatedOrderDataFiles));
    }
}
