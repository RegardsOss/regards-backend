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
package fr.cnes.regards.modules.feature.service.job;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureDeletionRequestEvent;

/**
 *
 * @author Sébastien Binda
 *
 */
public class DeletionEventListener implements IBatchHandler<FeatureDeletionRequestEvent> {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private long numberOfRequests = 0L;

    @Override
    public boolean validate(String tenant, FeatureDeletionRequestEvent message) {
        return true;
    }

    @Override
    public void handleBatch(String tenant, List<FeatureDeletionRequestEvent> messages) {
        LOGGER.info("TEST ------> handling {} deletion messages", messages.size());
        numberOfRequests = getNumberOfRequests() + messages.size();
    }

    public long getNumberOfRequests() {
        return numberOfRequests;
    }

    public void reset() {
        numberOfRequests = 0;
    }

}
