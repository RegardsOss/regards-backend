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
package fr.cnes.regards.modules.storagelight.service.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceRequest;
import fr.cnes.regards.modules.storagelight.domain.plugin.DefaultWorkingSubset;
import fr.cnes.regards.modules.storagelight.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storagelight.domain.plugin.IProgressManager;
import fr.cnes.regards.modules.storagelight.domain.plugin.IWorkingSubset;
import fr.cnes.regards.modules.storagelight.domain.plugin.PluginConfUpdatable;
import fr.cnes.regards.modules.storagelight.domain.plugin.StorageAccessModeEnum;

/**
 * @author Binda sébastien
 *
 */
@Plugin(author = "REGARDS Team", description = "Plugin handling the storage on local file system",
        id = "SimpleOnlineTest", version = "1.0", contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class SimpleOnlineDataStorage implements IOnlineDataStorage<DefaultWorkingSubset> {

    /**
     * Plugin parameter name of the storage base location as a string
     */
    public static final String BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME = "Storage_URL";

    /**
     * {@link IRuntimeTenantResolver} instance
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Base storage location url
     */
    @PluginParameter(name = BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, description = "Base storage location url to use",
            label = "Base storage location url")
    private String baseStorageLocationAsString;

    /**
     * storage base location as url
     */
    @SuppressWarnings("unused")
    private URL baseStorageLocation;

    /**
     * Plugin init method
     * @throws MalformedURLException
     */
    @PluginInit
    public void init() throws MalformedURLException {
        baseStorageLocation = new URL(baseStorageLocationAsString);
    }

    @Override
    public Collection<IWorkingSubset> prepare(Collection<FileReferenceRequest> FileReferenceRequest,
            StorageAccessModeEnum mode) {
        Collection<IWorkingSubset> workingSubSets = Lists.newArrayList();
        workingSubSets.add(new DefaultWorkingSubset(Sets.newHashSet(FileReferenceRequest)));
        return workingSubSets;
    }

    @Override
    public void store(DefaultWorkingSubset workingSubset, IProgressManager progressManager) {
        // because we use a parallel stream, we need to get the tenant now and force it before each doStore call
        String tenant = runtimeTenantResolver.getTenant();
        workingSubset.getFileReferenceRequests().stream().forEach(data -> {
            runtimeTenantResolver.forceTenant(tenant);
            doStore(progressManager, data);
        });
    }

    private void doStore(IProgressManager progressManager, FileReferenceRequest fileRefRequest) {
        fileRefRequest.getDestination()
                .setUrl(String.format("%s%s", baseStorageLocation.toString(), Paths
                        .get("/", fileRefRequest.getDestination().getUrl(), fileRefRequest.getMetaInfo().getChecksum())
                        .toString()));
        progressManager.storageSucceed(fileRefRequest, fileRefRequest.getMetaInfo().getFileSize());
    }

    @Override
    public void delete(DefaultWorkingSubset workingSubset, IProgressManager progressManager) {
    }

    @Override
    public InputStream retrieve(FileReference fileRef) throws IOException {
        return Files.newInputStream(Paths.get(fileRef.getLocation().getUrl()));
    }

    @Override
    public PluginConfUpdatable allowConfigurationUpdate(PluginConfiguration newConfiguration,
            PluginConfiguration currentConfiguration, boolean filesAlreadyStored) {
        return PluginConfUpdatable.allowUpdate();
    }
}
