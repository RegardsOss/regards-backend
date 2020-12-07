/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.exceptions;

import org.springframework.http.HttpStatus;

/**
 * List of the different possible types of error.
 *
 * @author gandrieu
 */
public enum ProcessingExceptionType {

    EXECUTION_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND),
    BATCH_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND),
    WORKDIR_CREATION_ERROR,
    WORKDIR_PREPARATION_ERROR,
    INTERNAL_DOWNLOAD_ERROR,
    EXTERNAL_DOWNLOAD_ERROR,
    STORE_OUTPUTFILE_ERROR,
    DELETE_OUTPUTFILE_ERROR,
    PERSIST_OUTPUT_FILES_ERROR,
    PERSIST_EXECUTION_STEP_ERROR,
    SEND_EXECUTION_RESULT_ERROR,
    NOTIFY_TIMEOUT_ERROR,
    MISSING_EXECUTION_CONTEXT_PARAM_ERROR,
    ;

    /** In case the error is used in an http request, this tells which http status to set for the response. */
    private final HttpStatus status;

    ProcessingExceptionType(HttpStatus status) {
        this.status = status;
    }

    ProcessingExceptionType() {
        this(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
