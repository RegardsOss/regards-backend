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

import java.util.UUID;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO : Class description
 *
 * @author Guillaume Andrieu
 *
 */
@SuppressWarnings("serial")
public abstract class ProcessingException extends RuntimeException {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingException.class);

    protected final UUID exceptionId;

    protected final ProcessingExceptionType type;

    protected final String desc;

    public ProcessingException(ProcessingExceptionType type, String desc) {
        super();
        this.exceptionId = UUID.randomUUID();
        this.type = type;
        this.desc = desc;
    }

    public ProcessingException(ProcessingExceptionType type, String desc, Throwable throwable) {
        this(type, desc);
        LOGGER.error("Processing error {} cause by:", exceptionId, throwable);
    }

    @Override
    public abstract String getMessage();

    public UUID getExceptionId() {
        return exceptionId;
    }

    public ProcessingExceptionType getType() {
        return type;
    }

    public static <T extends Throwable> Predicate<T> mustWrap() {
        return ProcessingException.class::isInstance;
    }
}
