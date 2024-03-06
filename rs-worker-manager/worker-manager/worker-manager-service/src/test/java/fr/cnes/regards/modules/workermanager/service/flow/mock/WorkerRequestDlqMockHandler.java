/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.workermanager.service.flow.mock;

import fr.cnes.regards.modules.workermanager.amqp.events.in.WorkerRequestDlqEvent;
import fr.cnes.regards.modules.workermanager.amqp.events.out.WorkerRequestEvent;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.Optional;

@Component
public class WorkerRequestDlqMockHandler extends AbstractEventMockHandler<WorkerRequestDlqEvent> {

    protected WorkerRequestDlqMockHandler() {
        super(WorkerRequestDlqEvent.class,
              Optional.of(WorkerRequestEvent.DLQ_ROOTING_KEY),
              Optional.of(WorkerRequestEvent.DLQ_ROOTING_KEY));
    }

    @Override
    public Class<WorkerRequestDlqEvent> getMType() {
        return WorkerRequestDlqEvent.class;
    }

    @Override
    public Errors validate(WorkerRequestDlqEvent message) {
        return null;
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return false;
    }

}
