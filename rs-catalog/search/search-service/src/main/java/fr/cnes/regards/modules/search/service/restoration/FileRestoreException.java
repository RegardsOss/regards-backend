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
package fr.cnes.regards.modules.search.service.restoration;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.search.service.ExceptionCauseEnum;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Stephane Cortine
 */
public class FileRestoreException extends ModuleException {

    private final ExceptionCauseEnum cause;

    public FileRestoreException(ExceptionCauseEnum cause, String message) {
        super(message);
        this.cause = cause;
    }

    public ResponseStatusException convertToResponseStatusException() {
        return new ResponseStatusException(cause.getCorrespondingHttpsStatus(), super.getMessage());
    }
}
