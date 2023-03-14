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
package fr.cnes.regards.modules.ltamanager.service.utils;

import fr.cnes.regards.modules.ltamanager.amqp.output.SubmissionResponseDtoEvent;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * Utils method to create submission responses
 *
 * @author Thibaud Michaudel
 **/
public final class SubmissionResponseDtoUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionResponseDtoUtils.class);

    private SubmissionResponseDtoUtils() {
    }

    // ----------------------
    // ------- EVENTS -------
    // ----------------------

    /**
     * Create an event with the given status using the request
     *
     * @param requestId    the id of the request
     * @param request      the request
     * @param status       the status given to the event
     * @param errorMessage the error message if the request ended in error
     * @return the event
     */
    public static SubmissionResponseDtoEvent createEvent(String requestId,
                                                         Optional<SubmissionRequest> request,
                                                         SubmissionResponseStatus status,
                                                         @Nullable String errorMessage) {

        String productId = null;
        String session = null;
        OffsetDateTime expiryDate = null;
        if (request.isEmpty()) {
            LOGGER.debug("No submission request found for id {} ", requestId);
        } else {
            productId = request.get().getProduct().getId();
            session = request.get().getSession();
            expiryDate = request.get().getExpiryDate();
        }

        return new SubmissionResponseDtoEvent(requestId, status, productId, expiryDate, session, errorMessage);
    }

    /**
     * Build the error message of the event from a set of errors
     *
     * @param errors the errors
     * @return the message as a string
     */
    public static String buildErrorMessage(Set<String> errors) {
        if (errors == null) {
            return null;
        }
        StringBuilder errorMessage = new StringBuilder();
        for (String error : errors) {
            errorMessage.append(error);
            errorMessage.append("  \\n");
        }
        return errorMessage.toString();
    }

    // ----------------------
    // -------- DTOS --------
    // ----------------------

    public static SubmissionResponseDto buildSuccessResponseDto(SubmissionRequest request) {
        String correlationId = request.getCorrelationId();
        LOGGER.debug("SubmissionRequest was successfully created from SubmissionRequestDto with correlationId \"{}\"",
                     correlationId);

        return new SubmissionResponseDto(correlationId,
                                         SubmissionResponseStatus.GRANTED,
                                         request.getProduct().getId(),
                                         request.getExpiryDate(),
                                         request.getSession(),
                                         null);
    }

}
