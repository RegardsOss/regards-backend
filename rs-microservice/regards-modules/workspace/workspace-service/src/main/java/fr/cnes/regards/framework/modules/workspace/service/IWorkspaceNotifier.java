/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.workspace.service;

import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * Interface allowing us to use notification module from rs-admin without adding cyclic git repository dependency
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IWorkspaceNotifier {

    /**
     * Send an error notification from the given parameters
     */
    void sendErrorNotification(String sender, String message, String title, DefaultRole role);

    /**
     * Send a warning notification from the given parameters
     */
    void sendWarningNotification(String sender, String message, String title, DefaultRole role);
}
