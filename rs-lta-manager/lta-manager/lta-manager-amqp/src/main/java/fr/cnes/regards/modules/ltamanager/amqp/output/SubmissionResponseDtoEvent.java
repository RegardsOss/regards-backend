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
package fr.cnes.regards.modules.ltamanager.amqp.output;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseStatus;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;

/**
 * See {@link SubmissionResponseDto}
 *
 * @author Iliana Ghazali
 **/
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class SubmissionResponseDtoEvent extends SubmissionResponseDto implements ISubscribable {

    public SubmissionResponseDtoEvent(String correlationId,
                                      SubmissionResponseStatus responseStatus,
                                      @Nullable String id,
                                      @Nullable String message) {
        super(correlationId, responseStatus, id, message);
    }

    public SubmissionResponseDtoEvent(String correlationId,
                                      SubmissionResponseStatus responseStatus,
                                      @Nullable String id,
                                      @Nullable OffsetDateTime expires,
                                      @Nullable String session,
                                      @Nullable String message) {
        super(correlationId, responseStatus, id, expires, session, message);
    }

}
