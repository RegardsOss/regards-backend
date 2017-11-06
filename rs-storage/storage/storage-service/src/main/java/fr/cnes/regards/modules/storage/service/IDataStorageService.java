package fr.cnes.regards.modules.storage.service;

import java.io.IOException;
import java.util.Collection;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.storage.plugin.datastorage.IDataStorage;
import fr.cnes.regards.modules.storage.plugin.datastorage.PluginStorageInfo;

/**
 * Contract that should be respected by the services handling {@link IDataStorage}
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IDataStorageService {

    /**
     * Retrieve monitoring information on each and every one of IDataStorage plugin which are active.
     */
    Collection<PluginStorageInfo> getMonitoringInfos() throws ModuleException, IOException;

    /**
     * Periodically scheduled task that monitor {@link IDataStorage}s and notify users when a data storage reaches its disk usage threshold. This method go threw all active tenants.
     */
    void monitorDataStorages();
}
