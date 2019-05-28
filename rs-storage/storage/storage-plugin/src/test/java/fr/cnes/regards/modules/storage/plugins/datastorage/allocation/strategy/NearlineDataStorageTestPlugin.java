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
package fr.cnes.regards.modules.storage.plugins.datastorage.allocation.strategy;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.plugin.DataStorageAccessModeEnum;
import fr.cnes.regards.modules.storage.domain.plugin.INearlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IProgressManager;
import fr.cnes.regards.modules.storage.domain.plugin.PluginConfUpdatable;
import fr.cnes.regards.modules.storage.domain.plugin.WorkingSubsetWrapper;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalWorkingSubset;

/**
 * Nearline datastorage plugin for tests
 *
 * @author sbinda
 *
 */
@Plugin(author = "REGARDS Team", description = "NEarline test plugin.", id = NearlineDataStorageTestPlugin.PLUGIN_ID,
        version = "1.0", contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class NearlineDataStorageTestPlugin implements INearlineDataStorage<LocalWorkingSubset> {

    public final static String PLUGIN_ID = "NearlineTest";

    @Override
    public WorkingSubsetWrapper<LocalWorkingSubset> prepare(Collection<StorageDataFile> dataFiles,
            DataStorageAccessModeEnum mode) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean canDelete() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void delete(LocalWorkingSubset workingSubset, IProgressManager progressManager) {
        // TODO Auto-generated method stub

    }

    @Override
    public void store(LocalWorkingSubset workingSubset, Boolean replaceMode, IProgressManager progressManager) {
        // TODO Auto-generated method stub

    }

    @Override
    public Long getTotalSpace() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void retrieve(LocalWorkingSubset workingSubset, Path destinationPath, IProgressManager progressManager) {
        // TODO Auto-generated method stub

    }

    @Override
    public PluginConfUpdatable allowConfigurationUpdate(PluginConfiguration newConfiguration,
            PluginConfiguration currentConfiguration, boolean filesAlreadyStored) {
        return PluginConfUpdatable.allowUpdate();
    }

    @Override
    public Map<String, Object> getDiagnosticInfo() {
        return null;
    }

}