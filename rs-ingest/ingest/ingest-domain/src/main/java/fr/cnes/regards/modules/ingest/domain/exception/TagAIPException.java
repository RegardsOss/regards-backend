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
package fr.cnes.regards.modules.ingest.domain.exception;

import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.modules.ingest.domain.request.IngestErrorType;

/**
 * ProcessingStepException thrown by TaggingStep
 *
 * @author Sébastien Binda
 */
public class TagAIPException extends ProcessingStepException {

    public TagAIPException(IngestErrorType errorType, String message, Throwable cause) {
        super(errorType, message, cause);
    }

    public TagAIPException(IngestErrorType errorType, String message) {
        this(errorType, message, null);
    }

    public TagAIPException(IngestErrorType errorType, Throwable cause) {
        this(errorType, null, cause);
    }

}
