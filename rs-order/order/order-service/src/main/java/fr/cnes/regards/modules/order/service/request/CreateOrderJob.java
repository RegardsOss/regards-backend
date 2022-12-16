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
package fr.cnes.regards.modules.order.service.request;

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.order.dto.input.OrderRequestDto;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * This job creates orders from {@link OrderRequestDto}
 *
 * @author Iliana Ghazali
 **/
public class CreateOrderJob extends AbstractJob<Void> {

    public static final String ORDER_REQUEST_EVENT = "ORDER_REQUEST_EVENT";

    private List<OrderRequestDto> orderRequestDto;

    @Autowired
    private OrderRequestService orderRequestService;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
        throws JobParameterMissingException, JobParameterInvalidException {
        this.orderRequestDto = getValue(parameters, ORDER_REQUEST_EVENT, new TypeToken<List<OrderRequestDto>>() {

        }.getType());
    }

    @Override
    public void run() {
        logger.debug("[{}] SubmissionDeleteExpiredJob starts", jobInfoId);
        long start = System.currentTimeMillis();
        orderRequestService.createOrderFromRequests(orderRequestDto);
        logger.debug("[{}] SubmissionDeleteExpiredJob ended in {}ms.", jobInfoId, System.currentTimeMillis() - start);
    }

}
