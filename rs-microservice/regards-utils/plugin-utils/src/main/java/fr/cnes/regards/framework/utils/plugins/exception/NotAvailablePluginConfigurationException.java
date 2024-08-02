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
package fr.cnes.regards.framework.utils.plugins.exception;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;

/**
 * @author sbinda
 */
public class NotAvailablePluginConfigurationException extends ModuleException {

    public NotAvailablePluginConfigurationException() {
        super();
    }

    public NotAvailablePluginConfigurationException(String message, Throwable cause, boolean enableSuppression, boolean writeStackTrace) {
        super(message, cause, enableSuppression, writeStackTrace);
    }

    public NotAvailablePluginConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotAvailablePluginConfigurationException(String message) {
        super(message);
    }

    public NotAvailablePluginConfigurationException(Throwable cause) {
        super(cause);
    }

}
