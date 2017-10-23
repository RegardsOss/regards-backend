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
package fr.cnes.regards.framework.modules.jobs.domain.exception;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * Exception to be thrown when a parameter required for a job execution is missing
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@SuppressWarnings("serial")
public class JobParameterMissingException extends ModuleException {

    /**
     * @param pErrorMessage
     */
    public JobParameterMissingException(String pErrorMessage) {
        super(pErrorMessage);
    }

    /**
     * @param pCause
     */
    public JobParameterMissingException(Throwable pCause) {
        super(pCause);
    }

}
