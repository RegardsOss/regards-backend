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
package fr.cnes.regards.framework.modules.workspace.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import fr.cnes.regards.framework.modules.workspace.domain.WorkspaceMonitoringInformation;

/**
 * Allows to use a workspace inside the application. This workspace should be separated by tenant.
 *
 * @author svissier
 *
 */
public interface IWorkspaceService {

    /**
     * Writes the {@link InputStream} content to the workspace into a file named fileName.
     * @param is
     * @param fileName
     * @throws IOException
     */
    void setIntoWorkspace(InputStream is, String fileName) throws IOException;

    /**
     * Retrieves the file which name is fileName from the workspace. Take care that the returned InputStream is to be closed by the caller.
     * @param fileName
     * @return new input stream from the file into the workspace.
     * @throws IOException
     */
    InputStream retrieveFromWorkspace(String fileName) throws IOException;

    /**
     * Removes the file which name is fileName from the workspace.
     * @param fileName
     * @throws IOException
     */
    void removeFromWorkspace(String fileName) throws IOException;

    /**
     * @return a newly created directory inside the workspace that you have to fully handle. Adding files into it should be done manually.
     * @throws IOException
     */
    Path getPrivateDirectory() throws IOException;

    /**
     * @return Monitoring information on the workspace.
     * @throws IOException
     */
    WorkspaceMonitoringInformation getMonitoringInformation() throws IOException;

    /**
     *
     */
    Path getMicroserviceWorkspace();
}
