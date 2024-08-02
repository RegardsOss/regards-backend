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

package fr.cnes.regards.modules.search.service.availability;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.search.service.ExceptionCauseEnum;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exceptions throws in AvailabilityService. Can be convert in ResponseStatusException easily
 *
 * @author tguillou
 */
public class FileAvailabilityException extends ModuleException {

    private final ExceptionCauseEnum notAvailabilityCause;

    public FileAvailabilityException(ExceptionCauseEnum notAvailabilityCause, String message) {
        super(message);
        this.notAvailabilityCause = notAvailabilityCause;
    }

    public ExceptionCauseEnum getNotAvailabilityCause() {
        return notAvailabilityCause;
    }

    /**
     * Throw a responseStatusException is an easy way to return a custom status with custom message
     * without "modifying" endpoint nominal response dto, or create custom spring controller advices.
     */
    public ResponseStatusException convertToResponseStatusException() {
        return new ResponseStatusException(notAvailabilityCause.getCorrespondingHttpsStatus(), super.getMessage());
    }
}
