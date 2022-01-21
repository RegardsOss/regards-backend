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
package fr.cnes.regards.modules.ingest.service.flow;

import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Common handler behaviour
 *
 * @author Marc SORDI
 */
public abstract class AbstractRequestFlowHandler<T extends ISubscribable> implements IBatchHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRequestFlowHandler.class);

    @Override
    public Errors validate(T message) {
        return null;
    }

    @Override
    public void handleBatch(List<T> messages) {
        LOGGER.trace("Processing bulk of {} items", messages.size());
        long start = System.currentTimeMillis();
        processBulk(messages);
        if (!messages.isEmpty()) {
            LOGGER.debug("{} items registered in {} ms", messages.size(), System.currentTimeMillis() - start);
        }
    }

    protected abstract void processBulk(List<T> items);
}
