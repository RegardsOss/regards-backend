/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.dao.stubs;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.test.repository.RepositoryStub;
import fr.cnes.regards.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;

/**
 * Plugin Repository stub
 *
 * @author cmertz
 *
 */
@Repository
@Profile("test")
@Primary
public class PluginConfigurationRepositoryStub extends RepositoryStub<PluginConfiguration>
        implements IPluginConfigurationRepository {

    /**
     * A list of plugin parameters
     */
    private final ArrayList<PluginParameter> pluginParameters1 = new ArrayList<>();

    /**
     *
     */
    public PluginConfigurationRepositoryStub() {
        this.pluginParameters1.add(new PluginParameter("name1", "val1"));
        this.pluginParameters1.add(new PluginParameter("name2", "val2"));
        this.entities
                .add(new PluginConfiguration(new PluginMetaData(), "conf plugion de test", this.pluginParameters1, 1));
    }

    @Override
    public List<PluginConfiguration> findByPluginIdAndTenantOrderByPriorityOrderDesc(String pPluginId) {
        // TODO Auto-generated method stub
        return null;
    }

}
