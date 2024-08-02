/* Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.web.advice;

import fr.cnes.regards.modules.processing.exceptions.ProcessingException;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

import static fr.cnes.regards.modules.processing.utils.TimeUtils.nowUtc;

/**
 * This class is the configuration for error formatting in the http layer.
 *
 * @author gandrieu
 */
@ControllerAdvice
public class ErrorFormatterControllerAdvice {

    @Value
    static class ErrorStructure {

        UUID errorId;

        String message;

        OffsetDateTime time;
    }

    @ExceptionHandler(ProcessingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ResponseEntity<ErrorStructure> processValidationError(ProcessingException e) {
        return new ResponseEntity<>(new ErrorStructure(e.getExceptionId(), e.getMessage(), nowUtc()),
                                    e.getType().getStatus());
    }
}