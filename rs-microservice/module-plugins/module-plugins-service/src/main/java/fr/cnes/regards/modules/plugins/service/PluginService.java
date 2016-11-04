/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.service;

import java.util.List;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.plugins.domain.IPluginType;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 *
 * 
 *
 * @author cmertz
 */
@Service
public class PluginService implements IPluginService {

    @Override
    public List<String> getPluginTypes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PluginMetaData> getPlugins() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PluginMetaData> getPluginsByType(String pPluginType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T getPlugin(Long pPluginConfigurationId, Class<T> pReturnInterfaceType) throws PluginUtilsException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T getFirstPluginByType(IPluginType pType, Class<T> pReturnInterfaceType) throws PluginUtilsException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PluginMetaData getPluginMetaDataById(String pPluginImplId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PluginConfiguration savePluginConfiguration(PluginConfiguration pPluginConfiguration)
            throws PluginUtilsException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PluginConfiguration getPluginConfiguration(Long pId) throws PluginUtilsException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PluginConfiguration> getPluginConfigurationsByType(IPluginType pType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deletePluginConfiguration(Long pPluginId) throws PluginUtilsException {
        // TODO Auto-generated method stub

    }

    @Override
    public PluginConfiguration updatePluginConfiguration(PluginConfiguration pPlugin) throws PluginUtilsException {
        // TODO Auto-generated method stub
        return null;
    }

}
