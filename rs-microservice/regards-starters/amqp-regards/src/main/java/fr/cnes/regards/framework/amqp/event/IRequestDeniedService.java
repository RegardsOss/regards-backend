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
package fr.cnes.regards.framework.amqp.event;

import fr.cnes.regards.framework.amqp.batch.dto.BatchMessage;
import org.springframework.amqp.core.Message;

import java.util.Optional;

/**
 * Build an optional AMQP response for an invalid message, i.e. if the batch handler rejected the input message, an
 * output message containing the error cause will be sent to a dedicated response handler.
 *
 * @author Marc SORDI
 */
public interface IRequestDeniedService {

    default <R extends ISubscribable> Optional<R> buildNotConvertedDeniedResponse(Message message,
                                                                                  String errorMessage) {
        return Optional.empty();
    }

    default <R extends ISubscribable> Optional<R> buildInvalidDeniedResponse(BatchMessage message,
                                                                             String errorMessage) {
        return Optional.empty();
    }
}
