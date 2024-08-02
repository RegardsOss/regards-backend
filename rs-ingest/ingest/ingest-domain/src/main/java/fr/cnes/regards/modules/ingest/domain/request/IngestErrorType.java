/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation) either version 3 of the License) or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful)
 * but WITHOUT ANY WARRANTY) without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.ingest.domain.request;

import fr.cnes.regards.framework.modules.jobs.domain.step.ErrorType;

/**
 * Error type associated to ingest requests in error. Each type is composed of a step and origin.
 * The step indicates the corresponding ongoing processing
 * {@link fr.cnes.regards.modules.ingest.domain.request.InternalRequestState},
 * and origin is an optional part describing the identified error origin.
 *
 * @author Iliana Ghazali
 **/
public enum IngestErrorType implements ErrorType {

    INITIAL,

    INITIAL_CHECKSUM,

    INITIAL_SIP_ALREADY_EXISTS,

    INITIAL_NOT_FIRST_VERSION_IGNORE,

    INITIAL_NOT_FIRST_VERSION_MANUAL,

    INITIAL_UNKNOWN_VERSIONING,

    PREPROCESSING,

    VALIDATION,

    GENERATION,

    METADATA,

    TAGGING,

    POSTPROCESSING,

    FINAL,

    UPDATE,

    DELETE,

    AIP_DUMP,

    NOTIFICATION,

    DISSEMINATION,

    UNEXPECTED

}
