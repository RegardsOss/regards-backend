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

import fr.cnes.regards.modules.processing.domain.POutputFile;
import fr.cnes.regards.modules.processing.exceptions.ProcessingException;
import fr.cnes.regards.modules.processing.exceptions.ProcessingExceptionType;

/**
 * This class is for exception occurring in the context of an output file.
 *
 * @author gandrieu
 */
public class ProcessingOutputFileException extends ProcessingException {

    protected final POutputFile outFile;

    public ProcessingOutputFileException(ProcessingExceptionType type, POutputFile outFile, String message) {
        super(type, message);
        this.outFile = outFile;
    }

    public ProcessingOutputFileException(ProcessingExceptionType type,
                                         POutputFile outFile,
                                         String message,
                                         Throwable throwable) {
        super(type, message, throwable);
        this.outFile = outFile;
    }

    public POutputFile getOutputFile() {
        return outFile;
    }

    @Override
    public final String getMessage() {
        return String.format("id=%s type=%s exec=%s outFile=%s message=%s",
                             this.exceptionId,
                             this.type,
                             this.outFile.getExecId(),
                             this.outFile.getId(),
                             this.desc);
    }
}
