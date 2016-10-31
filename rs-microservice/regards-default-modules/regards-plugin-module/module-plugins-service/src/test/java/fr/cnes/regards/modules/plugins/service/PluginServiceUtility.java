/**LICENSE_PLACEHOLDER*/
package fr.cnes.regards.modules.plugins.service;

import java.util.Arrays;
import java.util.List;

import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginParametersFactory;

/***
 * Constants and datas for unit testing of plugin's Service.
 * 
 * @author cmertz
 *
 */
public class PluginServiceUtility {

    /**
     * Project used for test
     */
    protected static final String PROJECT = "test1";

    /**
     * An id constant {@link String}
     */
    protected static final Long AN_ID = new Long(33);

    /**
     * Version
     */
    protected static final String VERSION = "12345-6789-11";

    /**
     * Role used for test
     */
    protected static final String USERROLE = "USERROLE";

    /**
     * RED constant {@link String}
     */
    protected static final String RED = "red";

    /**
     * GREEN constant {@link String}
     */
    protected static final String GREEN = "green";

    /**
     * BLUE constant {@link String}
     */
    protected static final String BLUE = "blue";

    /**
     * BLUE constant {@link String}
     */
    protected static final String INVALID_JWT = "Invalid JWT";

    /**
     * HELLO constant {@link String}
     */
    protected static final String HELLO = "hello";

    /**
     * RESULT constant {@link String}
     */
    protected static final String RESULT = "result=";

    /**
     * 5 constant {@link String}
     */
    protected static final int CINQ = 5;

    /**
     * 4 constant {@link String}
     */
    protected static final int QUATRE = 4;

    /**
     * A plugin identifier constant {@link String}
     */
    protected static final String PLUGIN_PARAMETER_ID = "aParameterPlugin";

    /**
     * A {@link List} of values
     */
    protected static final List<String> DYNAMICVALUES = Arrays.asList(RED, BLUE, GREEN);

    /**
     * A {@link PluginParameter}
     */
    protected static final List<PluginParameter> DYNAMICPARAMETERS = PluginParametersFactory.build()
            .addParameter("param11", "value11").addParameterDynamic("coeff", "0").addParameter("isActive", "true")
            .addParameterDynamic("suffix", RED, DYNAMICVALUES).getParameters();

    /**
     * A list of {@link PluginParameter}
     */
    protected static final List<PluginParameter> INTERFACEPARAMETERS = PluginParametersFactory.build()
            .addParameter("param31", "value31").addParameter("param32", "value32").addParameter("param33", "value33")
            .addParameter("param34", "value34").addParameter("param35", "value35").addParameterDynamic("coeff", "3")
            .addParameter("isActive", "true").addParameter("suffix", "Toulouse").getParameters();

    /**
     * A {@link PluginConfiguration}
     */
    private PluginConfiguration pluginConfiguration1 = new PluginConfiguration(this.getPluginMetaData(),
            "a configuration", INTERFACEPARAMETERS, 0);

    /**
     * A list of {@link PluginParameter} with a dynamic {@link PluginParameter}
     */
    private PluginConfiguration pluginConfiguration2 = new PluginConfiguration(this.getPluginMetaData(),
            "second configuration", DYNAMICPARAMETERS, 0);
    
    /**
     * A list of {@link PluginParameter} without parameters.
     */
    private PluginConfiguration pluginConfiguration3 = new PluginConfiguration(this.getPluginMetaData(),
            "third configuration", 99);

    protected PluginMetaData getPluginMetaData() {
        final PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setClass(Integer.class);
        pluginMetaData.setPluginId("aSamplePlugin");
        pluginMetaData.setAuthor("CS-SI");
        pluginMetaData.setVersion(VERSION);
        return pluginMetaData;
    }

    protected PluginConfiguration getPluginConfigurationWithParameters() {
        return pluginConfiguration1;
    }

    protected PluginConfiguration getPluginConfigurationWithDynamicParameter() {
        return pluginConfiguration2;
    }
    
    protected PluginConfiguration getPluginConfigurationWithoutParameters() {
        return pluginConfiguration3;
    }

    protected void resetId() {
        getPluginConfigurationWithDynamicParameter().setId(null);
        getPluginConfigurationWithDynamicParameter().getParameters().forEach(p -> p.setId(null));
        getPluginConfigurationWithParameters().setId(null);
        getPluginConfigurationWithParameters().getParameters().forEach(p -> p.setId(null));
    }

}
