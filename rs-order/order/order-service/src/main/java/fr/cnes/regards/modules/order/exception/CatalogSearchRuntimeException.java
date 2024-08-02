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
package fr.cnes.regards.modules.order.exception;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * Exception to inform about an error during catalog microservice search to retrieve information about
 * ordered data files. Runtime version of the exception to allows exception from iterable implementation.
 *
 * @author Sébastien Binda
 */
public class CatalogSearchRuntimeException extends RuntimeException {

    public CatalogSearchRuntimeException(ModuleException e) {
        super(e);
    }
}
