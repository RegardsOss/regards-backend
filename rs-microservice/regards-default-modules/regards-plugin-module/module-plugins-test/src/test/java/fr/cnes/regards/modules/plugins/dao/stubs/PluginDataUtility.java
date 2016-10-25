/**LICENSE_PLACEHOLDER*/
package fr.cnes.regards.modules.plugins.dao.stubs;

import java.util.Arrays;
import java.util.List;

import fr.cnes.regards.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginParametersFactory;

/***
 * Constants and datas for unit testing of plugin's DAO.
 * 
 * @author cmertz
 *
 */
public class PluginDataUtility {

    /**
     * Project used for test
     */
    static final protected String PROJECT = "test1";

    static final protected Long AN_ID = new Long(33);

    /**
     * Version
     */
    static final protected String VERSION = "12345-6789-11";

    /**
     * Role used for test
     */
    static final protected String USERROLE = "USERROLE";

    /**
     * RED constant {@link String}
     */
    static final protected String RED = "red";

    /**
     * GREEN constant {@link String}
     */
    static final protected String GREEN = "green";

    /**
     * BLUE constant {@link String}
     */
    static final protected String BLUE = "blue";

    /**
     * BLUE constant {@link String}
     */
    static final protected String INVALID_JWT = "Invalid JWT";

    /**
     * HELLO constant {@link String}
     */
    static final protected String HELLO = "hello";

    /**
     * RESULT constant {@link String}
     */
    static final protected String RESULT = "result=";

    /**
     * 5 constant {@link String}
     */
    public static final int CINQ = 5;

    /**
     * 4 constant {@link String}
     */
    public static final int QUATRE = 4;

    static final protected String pluginParameterId = "aParameterPlugin";

    /**
     * A {@link List} of values
     */
    static final List<String> DYNAMICVALUES = Arrays.asList(RED, BLUE, GREEN);

    /**
     * A {@link PluginParameter}
     */
    static final List<PluginParameter> DYNAMICPARAMETERS = PluginParametersFactory.build()
            .addParameter("param11", "value11").addParameterDynamic("coeff", "0")
            .addParameter("isActive", "true")
            .addParameterDynamic("suffix", RED, DYNAMICVALUES).getParameters();

    /**
     * A list of {@link PluginParameter}
     */
    static final List<PluginParameter> INTERFACEPARAMETERS = PluginParametersFactory.build()
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
     * A {@link PluginParameter} with a reference to a {@link PluginConfiguration}
     */
    private PluginParameter pluginParameter4 = new PluginParameter("param41", getPluginConfigurationWithParameters());

    PluginMetaData getPluginMetaData() {
        final PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setClass(Integer.class);
        pluginMetaData.setPluginId("aSamplePlugin");
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
//        PARAMETER2.getDynamicsValues().forEach(p -> p.setId(null));
    }

}
