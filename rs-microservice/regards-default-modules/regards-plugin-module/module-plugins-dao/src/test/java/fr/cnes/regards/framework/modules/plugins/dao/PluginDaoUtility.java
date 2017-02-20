/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.plugins.dao;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTest;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;

/***
 * Constants and datas for unit testing of plugin's DAO.
 *
 * @author Christophe Mertz
 *
 */
public class PluginDaoUtility extends AbstractDaoTest {

    /**
     * Class logger
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(PluginDaoUtility.class);

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
    static final PluginParameter A_PARAMETER = PluginParametersFactory.build().addParameter("param11", "value11")
            .getParameters().get(0);

    /**
     * A {@link List} of values
     */
    static final List<String> DYNAMICVALUES = Arrays.asList(RED, BLUE, GREEN);

    /**
     * A {@link List} of {@link PluginParameter}
     */
    static final List<PluginParameter> PARAMETERS2 = PluginParametersFactory.build()
            .addParameterDynamic("param-dyn21", RED, DYNAMICVALUES)
            .addParameterDynamic("param-dyn31", GREEN, DYNAMICVALUES).addParameter("param31", "value31")
            .addParameter("param51", "value51").addParameter("param61", "value61").getParameters();

    /**
     * A list of {@link PluginParameter}
     */
    static final List<PluginParameter> INTERFACEPARAMETERS = PluginParametersFactory.build()
            .addParameter("param41", "value41").addParameter("param42", "value42").addParameter("param43", "value43")
            .addParameter("param44", "value44").addParameter("param45", "value45").getParameters();

    /**
     * A {@link PluginConfiguration}
     */
    private static PluginConfiguration pluginConfiguration1 = new PluginConfiguration(getPluginMetaData(),
            "a configuration", INTERFACEPARAMETERS, 0);

    /**
     * A list of {@link PluginParameter} with a dynamic {@link PluginParameter}
     */
    private static PluginConfiguration pluginConfiguration2 = new PluginConfiguration(getPluginMetaData(),
            "second configuration", PARAMETERS2, 0);

    /**
     * IPluginConfigurationRepository
     */
    @Autowired
    protected IPluginConfigurationRepository pluginConfigurationRepository;

    /**
     * IPluginParameterRepository
     */
    @Autowired
    protected IPluginParameterRepository pluginParameterRepository;

    static PluginMetaData getPluginMetaData() {
        final PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setPluginClassName(Integer.class.getCanonicalName());
        pluginMetaData.setPluginId("plugin-id");
        pluginMetaData.setAuthor("CS-SI");
        pluginMetaData.setVersion(VERSION);
        return pluginMetaData;
    }

    public static PluginConfiguration getPluginConfigurationWithParameters() {
        return pluginConfiguration1;
    }

    public static PluginConfiguration getPluginConfigurationWithDynamicParameter() {
        return pluginConfiguration2;
    }

    protected void cleanDb() {
        pluginConfigurationRepository.deleteAll();
        pluginParameterRepository.deleteAll();
        resetId();
    }

    protected static void resetId() {
        getPluginConfigurationWithDynamicParameter().setId(null);
        getPluginConfigurationWithDynamicParameter().getParameters().forEach(p -> p.setId(null));

        getPluginConfigurationWithParameters().setId(null);
        getPluginConfigurationWithParameters().getParameters().forEach(p -> p.setId(null));

        INTERFACEPARAMETERS.forEach(p -> p.setId(null));
    }

    protected void displayParams() {
        LOGGER.info("=====> parameter");
        pluginParameterRepository.findAll().forEach(p -> LOGGER.info("name=" + p.getName() + "-value=" + p.getValue()
                + "-nb dyns=" + p.getDynamicsValuesAsString().size()));
        for (PluginParameter pP : pluginParameterRepository.findAll()) {
            if ((pP.getDynamicsValues() != null) && !pP.getDynamicsValues().isEmpty()) {
                pP.getDynamicsValues().forEach(p -> LOGGER.info("-value=" + p.getValue()));
            }
        }
        LOGGER.info("<=====");
    }

}
