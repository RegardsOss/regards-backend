/**LICENSE_PLACEHOLDER*/
package fr.cnes.regards.modules.plugins.dao.stubs;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.test.repository.RepositoryStub;
import fr.cnes.regards.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginParametersFactory;

/***
 * {@link PluginConfiguration} Repository stub.
 * 
 * @author Christophe Mertz
 *
 */
@Repository
@Profile("test")
@Primary
public class PluginConfigurationRepositoryStub extends RepositoryStub<PluginConfiguration>
        implements IPluginConfigurationRepository {

    public PluginConfigurationRepositoryStub() {
        getEntities().add(getPluginConfigurationWithDynamicParameter());
        getEntities().add(getPluginConfigurationWithParameters());
    }

    /**
     * An id constant {@link String}
     */
    static final Long AN_ID = new Long(33);
    
    /**
     * Version
     */
    static final String VERSION = "12345-6789-11";

    /**
     * RED constant {@link String}
     */
    static final String RED = "red";

    /**
     * GREEN constant {@link String}
     */
    static final String GREEN = "green";

    /**
     * BLUE constant {@link String}
     */
    static final String BLUE = "blue";

    /**
     * A {@link PluginParameter}
     */
    static final PluginParameter PARAMETER1 = PluginParametersFactory.build().addParameter("param11", "value11")
            .getParameters().get(0);

    /**
     * A {@link List} of values
     */
    static final List<String> DYNAMICVALUES = Arrays.asList(RED, BLUE, GREEN);

    /**
     * A {@link PluginParameter}
     */
    static final PluginParameter PARAMETER2 = PluginParametersFactory.build()
            .addParameterDynamic("param-dyn21", RED, DYNAMICVALUES).getParameters().get(0);

    /**
     * A list of {@link PluginParameter}
     */
    static final List<PluginParameter> INTERFACEPARAMETERS = PluginParametersFactory.build()
            .addParameter("param31", "value31").addParameter("param32", "value32").addParameter("param33", "value33")
            .addParameter("param34", "value34").addParameter("param35", "value35").getParameters();

    /**
     * A {@link PluginConfiguration}
     */
    private PluginConfiguration pluginConfiguration1 = new PluginConfiguration(this.getPluginMetaData(),
            "a configuration", INTERFACEPARAMETERS, 0);

    /**
     * A list of {@link PluginParameter} with a dynamic {@link PluginParameter}
     */
    private PluginConfiguration pluginConfiguration2 = new PluginConfiguration(this.getPluginMetaData(),
            "second configuration", Arrays.asList(PARAMETER1, PARAMETER2), 0);

    public PluginMetaData getPluginMetaData() {
        final PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setClass(Integer.class);
        pluginMetaData.setPluginId("plugin-id");
        pluginMetaData.setAuthor("CS-SI");
        pluginMetaData.setVersion(VERSION);
        return pluginMetaData;
    }

    public PluginConfiguration getPluginConfigurationWithParameters() {
        pluginConfiguration1.setId(AN_ID);
        return pluginConfiguration1;
    }

    public PluginConfiguration getPluginConfigurationWithDynamicParameter() {
        pluginConfiguration2.setId(new Long(AN_ID.longValue()+1));
        return pluginConfiguration2;
    }

    @Override
    public List<PluginConfiguration> findByPluginIdOrderByPriorityOrderDesc(String pPluginId) {
        return Arrays.asList(pluginConfiguration1, pluginConfiguration2);
    }

}
