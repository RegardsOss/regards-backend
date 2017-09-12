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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.check;

import java.io.File;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;

public class CheckInPluginFalse implements ICheckFilePlugin {

    // Attributes
    private String productName_;

    private int productVersion_;

    private int fileVersion_;

    private String logFilePath_;

    /**
     * @DM SIPNG-DM-0060-CN : creation
     * @since 1.3
     */
    private String nodeIdentifier_;

    /**
     * Constructeur de la classe
     * 
     * @since 1.2
     * 
     */
    public CheckInPluginFalse() {
        super();
    }

    @Override
    public int getFileVersion() {
        return fileVersion_;
    }

    @Override
    public String getLogFile() {
        return logFilePath_;
    }

    @Override
    public String getProductName() {
        return productName_;
    }

    @Override
    public int getProductVersion() {
        return productVersion_;
    }

    @Override
    public String getNodeIdentifier() {
        return nodeIdentifier_;
    }

    @Override
    public boolean runPlugin(File pFiletoCheck, String pDataSetId) throws ModuleException {

        return false;
    }
}
