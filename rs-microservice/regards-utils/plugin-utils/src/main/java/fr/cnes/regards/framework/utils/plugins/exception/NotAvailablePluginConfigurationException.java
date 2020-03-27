/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

/**
 * @author sbinda
 *
 */
@SuppressWarnings("serial")
public class NotAvailablePluginConfigurationException extends Exception {

    public NotAvailablePluginConfigurationException() {
        super();
    }

    public NotAvailablePluginConfigurationException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
        super(arg0, arg1, arg2, arg3);
    }

    public NotAvailablePluginConfigurationException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public NotAvailablePluginConfigurationException(String arg0) {
        super(arg0);
    }

    public NotAvailablePluginConfigurationException(Throwable arg0) {
        super(arg0);
    }

}
