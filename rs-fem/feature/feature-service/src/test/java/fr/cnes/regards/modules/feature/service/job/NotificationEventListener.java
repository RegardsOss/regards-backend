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

import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureNotificationRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * @author Sébastien Binda
 */
public class NotificationEventListener implements IBatchHandler<FeatureNotificationRequestEvent> {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private long numberOfRequests = 0L;

    @Override
    public Errors validate(FeatureNotificationRequestEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<FeatureNotificationRequestEvent> messages) {
        LOGGER.info("TEST ------> handling {} notification messages", messages.size());
        numberOfRequests = getNumberOfRequests() + messages.size();
    }

    public long getNumberOfRequests() {
        return numberOfRequests;
    }

    public void reset() {
        numberOfRequests = 0;
    }

}
