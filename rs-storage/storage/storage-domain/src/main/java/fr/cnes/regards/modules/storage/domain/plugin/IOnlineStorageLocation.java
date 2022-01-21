/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.domain.plugin;

import java.io.FileNotFoundException;
import java.io.InputStream;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.storage.domain.database.FileReference;

/**
 * Plugin to handle ONLINE storage location. <br/>
 * An online storage location is a location where file can be access synchronously and can be directly download.<br/>
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@PluginInterface(description = "Contract to respect by any ONLINE data storage plugin")
public interface IOnlineStorageLocation extends IStorageLocation {

    /**
     * Do retrieve action for the given {@link StorageDataFile}
     * @param data StorageDataFile to retrieve
     * @throws ModuleException
     */
    InputStream retrieve(FileReference fileReference) throws ModuleException, FileNotFoundException;

}
