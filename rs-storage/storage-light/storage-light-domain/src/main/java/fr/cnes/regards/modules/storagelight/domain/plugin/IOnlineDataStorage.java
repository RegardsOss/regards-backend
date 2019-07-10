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
package fr.cnes.regards.modules.storagelight.domain.plugin;

import java.io.IOException;
import java.io.InputStream;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@PluginInterface(description = "Contract to respect by any ONLINE data storage plugin")
public interface IOnlineDataStorage<T extends IWorkingSubset> extends IDataStorage<T> {

    /**
     * Do the retreive action for the given {@link StorageDataFile}
     * @param data StorageDataFile to retrieve
     */
    InputStream retrieve(FileReference fileReference) throws IOException;

}
