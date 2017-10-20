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
package fr.cnes.regards.modules.acquisition.plugins;

import java.io.File;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;

/**
 * @author Christophe Mertz
 *
 */
@PluginInterface(description = "Plugin to validate a product")
public interface ICheckFilePlugin {
    
    // TODO CMZ à renommer c'est plutôt validate ou check
    public boolean runPlugin(File fileToCheck, String dataSetId) throws ModuleException;
    
    // TODO CMZ à voir mais je pense que c'est inutile
    public int getFileVersion();

    // TODO CMZ util ?
    public String getLogFile();

    public String getProductName();

    // TODO CMZ util ?
    public int getProductVersion();

    // TODO CMZ util pour les tests pour générer descripteur ?
    public String getNodeIdentifier();
}
