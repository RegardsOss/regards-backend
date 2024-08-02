/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.delivery.service.order.zip.job;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.delivery.domain.exception.DeliveryOrderException;
import fr.cnes.regards.modules.delivery.domain.input.DeliveryRequest;
import fr.cnes.regards.modules.delivery.domain.order.zip.ZipDeliveryInfo;
import fr.cnes.regards.modules.delivery.service.order.zip.OrderDeliveryZipService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Job handling the execution of a {@link DeliveryRequest}. See {@link OrderDeliveryZipService}.
 *
 * @author Iliana Ghazali
 **/
public class OrderDeliveryZipJob extends AbstractJob<Void> {

    @Autowired
    private OrderDeliveryZipService deliveryZipService;

    @Autowired
    private OrderDeliveryZipJobProgressManager deliveryJobProgressManager;

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        DeliveryRequest deliveryRequest = deliveryJobProgressManager.findDeliveryRequestToProcess(jobInfoId);
        logger.info("[{}] OrderDeliveryZipJob of request '{}' starts...",
                    jobInfoId,
                    deliveryRequest.getCorrelationId());
        try {
            ZipDeliveryInfo uploadedZipInfo = deliveryZipService.makeDelivery(deliveryRequest);
            deliveryJobProgressManager.handleDeliverySuccess(deliveryRequest, uploadedZipInfo);
        } catch (DeliveryOrderException e) {
            deliveryJobProgressManager.handleDeliveryError(deliveryRequest, e);
            // send RsRuntimeException to make the job fail
            throw new RsRuntimeException(e);
        }
        logger.info("[{}] OrderDeliveryZipJob has ended successfully in {} ms.",
                    jobInfoId,
                    System.currentTimeMillis() - start);
    }

}
