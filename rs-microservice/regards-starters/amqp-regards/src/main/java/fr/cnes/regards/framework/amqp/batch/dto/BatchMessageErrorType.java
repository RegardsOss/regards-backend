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
package fr.cnes.regards.framework.amqp.batch.dto;

/**
 * List all the error types that could occur while processing a batch of AMQP messages. Note that it is important to
 * keep the labels simple and concise because they are used in notification titles.
 *
 * @author Iliana Ghazali
 **/
public enum BatchMessageErrorType {

    /**
     * The tenant cannot be found in the header or the message wrapper.
     */
    MISSING_TENANT("Missing tenant in AMQP message header."),

    /**
     * The message cannot be processed by the current handler.
     */
    MESSAGE_ORIGIN_MISMATCH("Incompatible message origin."),

    /**
     * The validation phase has failed, the message is denied.
     */
    INVALID_MESSAGE("Invalid AMQP message."),

    /**
     * The json message conversion phase has failed.
     */
    NOT_CONVERTED_MESSAGE("Failed AMQP message conversion."),

    /**
     * The validation of the AMQP message has failed unexpectedly.
     */
    UNEXPECTED_VALIDATION_FAILURE("Unexpected failure during the message validation."),

    /**
     * The processing of a batch of AMQP messages failed unexpectedly.
     */
    UNEXPECTED_BATCH_FAILURE("Unexpected AMQP batch processing failure.");

    private final String label;

    BatchMessageErrorType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
