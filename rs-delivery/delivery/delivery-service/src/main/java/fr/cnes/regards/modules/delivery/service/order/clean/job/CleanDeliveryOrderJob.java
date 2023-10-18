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
package fr.cnes.regards.modules.delivery.service.order.clean.job;

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.service.order.clean.CleanOrderService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * Job to perform cleaning tasks after delivery requests in error.
 *
 * @author Iliana Ghazali
 **/
public class CleanDeliveryOrderJob extends AbstractJob<Void> {

    public static final String DELIVERY_REQUESTS_TO_CLEAN = "DELIVERY_REQUESTS_TO_CLEAN";

    private List<DeliveryRequest> deliveryRequests;

    @Autowired
    private CleanOrderService cleanOrderService;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        this.deliveryRequests = getValue(parameters,
                                         DELIVERY_REQUESTS_TO_CLEAN,
                                         new TypeToken<List<DeliveryRequest>>() {

                                         }.getType());
    }

    @Override
    public void run() {
        logger.debug("[{}] Cancel delivery order job starts. Handle {} orders in its delivery.",
                     jobInfoId,
                     deliveryRequests.size());
        long start = System.currentTimeMillis();

        cleanOrderService.cleanDeliveryOrder(deliveryRequests);

        logger.info("[{}] Clean delivery order job has ended in {} ms.", jobInfoId, System.currentTimeMillis() - start);
    }

}
