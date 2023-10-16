/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.service.submission.update;

import fr.cnes.regards.framework.jpa.multitenant.lock.LockServiceTask;
import fr.cnes.regards.modules.delivery.service.schedulers.UpdateExpiredDeliveryRequestScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Task linked to {@link UpdateExpiredDeliveryRequestScheduler} to handle
 * expired {@link fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest}s.
 *
 * @author Iliana Ghazali
 **/
public class UpdateExpiredTask implements LockServiceTask<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateExpiredTask.class);

    private final UpdateExpiredService updateExpiredService;

    public UpdateExpiredTask(UpdateExpiredService updateExpiredService) {
        this.updateExpiredService = updateExpiredService;
    }

    @Override
    public Void run() {
        OffsetDateTime limitExpirationDate = OffsetDateTime.now(ZoneOffset.UTC);
        LOGGER.debug("Starting update expired task from '{}'.", limitExpirationDate);
        updateExpiredService.handleExpiredRequests(limitExpirationDate);
        LOGGER.debug("End of update expired task. Took {}ms.",
                     ChronoUnit.MILLIS.between(limitExpirationDate, OffsetDateTime.now(ZoneOffset.UTC)));

        return null;
    }
}
