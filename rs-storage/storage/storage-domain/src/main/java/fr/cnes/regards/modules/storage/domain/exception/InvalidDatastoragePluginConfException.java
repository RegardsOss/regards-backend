/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.domain.exception;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * Exception thrown when a PluginConfiguration of a Datastorage is needed and is not well configured.
 * @author sbinda
 */
@SuppressWarnings("serial")
public class InvalidDatastoragePluginConfException extends ModuleException {

    private static String errorMessage = "Invalid Datastorage Plugin configuration for plugin id %s";

    public InvalidDatastoragePluginConfException(Long datastoragePluginConfId) {
        super(String.format(errorMessage, datastoragePluginConfId));
    }

    public InvalidDatastoragePluginConfException(Long datastoragePluginConfId, Throwable cause) {
        super(String.format(errorMessage, datastoragePluginConfId), cause);
    }

}
