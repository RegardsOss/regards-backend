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
package fr.cnes.regards.modules.storage.plugin.allocation.strategy;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.IAllocationStrategy;

/**
 * Default multiple allocation strategy, allocate each file to all specified data storage.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Plugin(author = "REGARDS Team",
        description = "Allocation Strategy plugin that allocates files to multiple data storage",
        id = "DefaultMultipleAllocationStrategy", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3",
        owner = "CNES", url = "https://regardsoss.github.io/")
public class DefaultMultipleAllocationStrategy implements IAllocationStrategy {

    public static final String DATA_STORAGE_IDS_PARAMETER_NAME = "DATA_STORAGE_IDS";

    @PluginParameter(name = DATA_STORAGE_IDS_PARAMETER_NAME,
            description = "Ids of data storage configuration on which files will ALL be stored. Don't forget to include at least one ONLINE data storage for quicklooks",
            label = "data storage ids")
    private Set<Long> dataStorageIds;

    @Override
    public Multimap<Long, StorageDataFile> dispatch(Collection<StorageDataFile> dataFilesToHandle) {
        Multimap<Long, StorageDataFile> dispatched = HashMultimap.create(dataStorageIds.size(),
                                                                         dataFilesToHandle.size());
        for (Long dataStorageId : dataStorageIds) {
            for (StorageDataFile dataFile : dataFilesToHandle) {
                dataFile.increaseNotYetStoredBy();
                dispatched.put(dataStorageId, dataFile);
            }
        }
        return dispatched;
    }
}
