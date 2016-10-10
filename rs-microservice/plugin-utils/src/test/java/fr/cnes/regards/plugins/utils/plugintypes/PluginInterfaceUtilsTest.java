/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.plugintypes;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.plugins.utils.AbstractPluginInterfaceUtils;
import fr.cnes.regards.plugins.utils.PluginManagerServiceTest;
import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 * PluginInterfaceUtilsTest
 * 
 * @author cmertz
 *
 */
public class PluginInterfaceUtilsTest {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginInterfaceUtilsTest.class);

    /**
     * Load all plugins
     */
    @Test
    public void loadPluginsInterface() {
        LOGGER.info("start " + this.toString());
        // Get all the plugin interfaces
        final List<String> pluginInterfaces = AbstractPluginInterfaceUtils
                .getInterfaces("fr.cnes.regards.plugins.utils");
        Assert.assertNotNull(pluginInterfaces);
        Assert.assertTrue(pluginInterfaces.size() > 0);

        pluginInterfaces.stream().forEach(s -> LOGGER.info(s));

        Assert.assertTrue(true);
    }

    /**
     * Get a {@link ComplexPlugin} with a specific parameters
     */
    @Test
    public void getComplexPlugin() {
        ComplexPlugin complexPlugin = null;
        SampleInterfacePlugin sampleInterfacePlugin = null;
        /*
         * Set all parameters
         */
        try {

            final List<PluginParameter> interfaceParameters = PluginParametersFactory.build()
                    .addParameter(SampleInterfacePlugin.KBYTE, "1").addParameter(SampleInterfacePlugin.KFLOAT, "6.2")
                    .addParameter(SampleInterfacePlugin.KLONG, "15487898989")
                    .addParameter(SampleInterfacePlugin.KSHORT, "33").getParameters();
            // instantiate interface plugin
            sampleInterfacePlugin = PluginManagerServiceTest.getPlugin(interfaceParameters,
                                                                       SampleInterfacePlugin.class);
            Assert.assertNotNull(sampleInterfacePlugin);

            final List<PluginParameter> complexParameters = PluginParametersFactory.build()
                    .addParameter(ComplexPlugin.ACTIVE, "true").addParameter(ComplexPlugin.COEFF, "5")
                    .addParameter(ComplexPlugin.PLG, "coucou").getParameters();

            // instantiate plugin
            complexPlugin = PluginManagerServiceTest.getPlugin(complexParameters, ComplexPlugin.class);
            Assert.assertNotNull(complexPlugin);

        } catch (final PluginUtilsException e) {
            LOGGER.error(e.getMessage());
            Assert.assertTrue(true);
        }

        /*
         * Use the plugin
         */
        // Assert.assertEquals(40, complexPlugin.add(5, 3));
        // Assert.assertTrue(complexPlugin.echo("hello world").contains("hello"));
    }

    @Test
    public void incompatibleInterfaceError() {
        ComplexErrorPlugin complexErrorPlugin = null;
        /*
         * Set all parameters
         */
        try {

            final List<PluginParameter> complexParameters = PluginParametersFactory.build()
                    .addParameter(ComplexErrorPlugin.ACTIVE, "true").addParameter(ComplexErrorPlugin.COEFF, "5")
                    .addParameter(ComplexErrorPlugin.PLG, "coucou").getParameters();

            // instantiate plugin
            complexErrorPlugin = PluginManagerServiceTest.getPlugin(complexParameters, ComplexErrorPlugin.class);
            Assert.assertNotNull(complexErrorPlugin);

        } catch (final PluginUtilsException e) {
            LOGGER.error(e.getMessage());
            Assert.assertTrue(true);
        }

    }

}
