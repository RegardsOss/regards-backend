/**LICENSE_PLACEHOLDER*/
package fr.cnes.regards.modules.plugins.dao.stubs;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Profile;

import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginParametersFactory;

/***
 * {@link PluginParameter} Repository stub
 * 
 * @author cmertz
 *
 */

@Profile("test")
public class PluginParameterRepositoryStub {

    private PluginParameterRepositoryStub() {
        super();
    }

    private static PluginParameterRepositoryStub INSTANCE = null;

    public static PluginParameterRepositoryStub getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PluginParameterRepositoryStub();
        }
        return INSTANCE;
    }

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

    /**
     * A {@link PluginParameter} with a reference to a {@link PluginConfiguration}
     */
    private PluginParameter pluginParameter4 = new PluginParameter("param41", getPluginConfigurationWithParameters());

    public PluginMetaData getPluginMetaData() {
        final PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setClass(Integer.class);
        pluginMetaData.setPluginId("plugin-id");
        pluginMetaData.setAuthor("CS-SI");
        pluginMetaData.setVersion(VERSION);
        return pluginMetaData;
    }

    public PluginConfiguration getPluginConfigurationWithParameters() {
        pluginConfiguration1.setIsActive(true);
        return pluginConfiguration1;
    }

    public PluginConfiguration getPluginConfigurationWithDynamicParameter() {
        pluginConfiguration2.setIsActive(true);
        return pluginConfiguration2;
    }

    public PluginParameter getPluginParameterWithPluginConfiguration() {
        return pluginParameter4;
    }

    public void resetId() {
        getPluginConfigurationWithDynamicParameter().setId(null);
        getPluginConfigurationWithDynamicParameter().getParameters().forEach(p -> p.setId(null));
        getPluginConfigurationWithParameters().setId(null);
        getPluginConfigurationWithParameters().getParameters().forEach(p -> p.setId(null));
        PARAMETER2.getDynamicsValues().forEach(p -> p.setId(null));
    }

}
