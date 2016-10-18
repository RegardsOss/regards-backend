/**LICENSE_PLACEHOLDER*/
package fr.cnes.regards.modules.plugins.dao;

import java.util.Arrays;
import java.util.List;

import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginParametersFactory;

/***
 * {@link PluginConfiguration} unit testing domain persistence
 * 
 * @author cmertz
 *
 */
public class PluginDaoTestDataUtility {

    /**
     * Project used for test
     */
    static final String PROJECT = "test1";

    /**
     * Version
     */
    static final String VERSION = "12345-6789-11";

    /**
     * Role used for test
     */
    static final String USERROLE = "USERROLE";

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
     * BLUE constant {@link String}
     */
    static final String INVALID_JWT = "Invalid JWT";

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
     * A {@link PluginParameter} with a reference to a {@link PluginConfiguration}
     */
    private PluginParameter pluginParameter4 = new PluginParameter("param41", getPluginConfiguration());

    PluginMetaData getPluginMetaData() {
        final PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setClass(Integer.class);
        pluginMetaData.setPluginId("plugin-id");
        pluginMetaData.setAuthor("CS-SI");
        pluginMetaData.setVersion(VERSION);
        return pluginMetaData;
    }

    public PluginConfiguration getPluginConfiguration() {
        pluginConfiguration1.setIsActive(true);
        return pluginConfiguration1;
    }

    public PluginParameter getPluginParameterWithPluginConfiguration() {
        return pluginParameter4;
    }

}
