/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.util.Collection;

import com.google.common.collect.Multimap;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.storage.domain.database.DataFile;

/**
 * Those plugins are meant to decide which plugin IDataStorage should be use for a given aip and file.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@PluginInterface(description = "Interface for all AllocationStrategy plugin")
@FunctionalInterface
public interface IAllocationStrategy {

    Multimap<PluginConfiguration, DataFile> dispatch(Collection<DataFile> dataFilesToHandle);
}
