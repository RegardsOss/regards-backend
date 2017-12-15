/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;

/**
 * Exception for Service acquisition module.
 *
 * @author Christophe Mertz
 *         Use {@link JobRuntimeException}
 */
@Deprecated
public class AcquisitionRuntimeException extends RuntimeException {

    /**
     * Class Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionRuntimeException.class);

    /**
     * serialVersionUID field.
     */
    private static final long serialVersionUID = -8481161040712025468L;

    /**
     * Constructor
     *
     * @param cause the exception
     */
    public AcquisitionRuntimeException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructor
     *
     * @param message an error message
     * @param cause the exception
     */
    public AcquisitionRuntimeException(String message, Throwable cause) {
        super(message, cause);
        LOGGER.error(message, cause);
    }

    /**
     * Constructor
     *
     * @param mssage an error message
     */
    public AcquisitionRuntimeException(String message) {
        super(message);
    }

}
