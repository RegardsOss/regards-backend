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
package fr.cnes.regards.modules.processing.domain.exception;

import fr.cnes.regards.modules.processing.domain.PExecution;
import fr.cnes.regards.modules.processing.exceptions.ProcessingException;
import fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType;

/**
 * This class is for process exception occurring in the context of an execution.
 *
 * @author gandrieu
 */
public class ProcessingExecutionException extends ProcessingException {

    protected final PExecution exec;

    public ProcessingExecutionException(ProcessingExceptionType type, PExecution exec, String message) {
        super(type, message);
        this.exec = exec;
    }

    public ProcessingExecutionException(ProcessingExceptionType type,
                                        PExecution exec,
                                        String message,
                                        Throwable throwable) {
        super(type, message, throwable);
        this.exec = exec;
    }

    @Override
    public final String getMessage() {
        return String.format("id=%s type=%s exec=%s batch=%s message=%s",
                             this.exceptionId,
                             this.type,
                             this.exec.getId(),
                             this.exec.getBatchId(),
                             this.desc);
    }

    public PExecution getExec() {
        return exec;
    }

}
