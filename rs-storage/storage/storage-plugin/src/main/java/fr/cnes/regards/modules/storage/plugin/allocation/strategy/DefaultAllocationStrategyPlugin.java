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

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import fr.cnes.regards.framework.microservice.maintenance.MaintenanceException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.storage.dao.IPrioritizedDataStorageRepository;
import fr.cnes.regards.modules.storage.domain.database.DataStorageType;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.DispatchErrors;
import fr.cnes.regards.modules.storage.domain.plugin.IAllocationStrategy;

/**
 * Default Implementation of IAllocationStrategy.
 * Finds Online data storage with the highest priority(lowest value of {@link PrioritizedDataStorage#priority}) and associate all the dataFiles to it.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Plugin(author = "REGARDS Team",
        description = "Finds Online data storage with the highest priority and associate all the dataFiles to it.",
        id = "DefaultAllocationStrategyPlugin", version = "1.0", contact = "regards@c-s.fr", license = "GPLv3",
        owner = "CNES", url = "https://regardsoss.github.io/")
public class DefaultAllocationStrategyPlugin implements IAllocationStrategy {

    @Autowired
    private IPrioritizedDataStorageRepository pdsRepository;

    @Override
    public Multimap<Long, StorageDataFile> dispatch(Collection<StorageDataFile> dataFilesToHandle,
            DispatchErrors errors) {
        // first lets get the online data storages, then lets get only the active ones,
        // eventually order them and choose the one with the highest priority
        PluginConfiguration dataStorageConfToUse = pdsRepository
                .findAllByDataStorageTypeOrderByPriorityAsc(DataStorageType.ONLINE).stream()
                .filter(pds -> pds.getDataStorageConfiguration().isActive()).sorted().findFirst()
                .orElseThrow(() -> new MaintenanceException(
                        "There is no active plugin configuration of type IDataStorage"))
                .getDataStorageConfiguration();
        HashMultimap<Long, StorageDataFile> result = HashMultimap.create(2, dataFilesToHandle.size());

        dataFilesToHandle.forEach(dataFile -> result.put(dataStorageConfToUse.getId(), dataFile));
        return result;
    }

}
