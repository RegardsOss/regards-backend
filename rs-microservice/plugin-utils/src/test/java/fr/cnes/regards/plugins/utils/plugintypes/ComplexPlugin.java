/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.plugintypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.plugins.utils.ISamplePlugin;

/**
 * ISamplePlugin
 * 
 * @author cmertz
 *
 */
@Plugin(author = "CSSI", description = "Complex plugin test", id = "aComplexPlugin", version = "0.0.1")
public class ComplexPlugin implements ISamplePlugin {

    /**
     * constant PLG
     */
    static final String PLUGIN_PARAM = "plgInterface";

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ComplexPlugin.class);

    /**
     * A plugin with an annotation {@link PluginParameter}
     */
    @PluginParameter(description = "Plugin interface", name = PLUGIN_PARAM)
    private IComplexInterfacePlugin complexInterfacePlugin;

    /**
     * A {@link Integer} parameter
     */
    @PluginParameter(description = "int parameter", name = COEFF)
    private Integer coef = 0;

    /**
     * A {@link Boolean} parameter
     */
    @PluginParameter(description = "boolean parameter", name = ACTIVE)
    private Boolean isActive = Boolean.FALSE;

    @Override
    public String echo(String pMessage) {
        final StringBuffer str = new StringBuffer();
        if (this.isActive) {
            str.append(this.getClass().getName() + "-" + pMessage);
        } else {

            str.append(this.getClass().getName() + ":is not active");
        }
        return str.toString();
    }

    @Override
    public int add(int pFirst, int pSecond) {
        final float f = complexInterfacePlugin.mult(4,8);
        LOGGER.info("float=" + f);
        final int res = this.coef * (pFirst + pSecond);
        LOGGER.info("add result : " + res);
        return res;
    }

    public String echoPluginParameter() {
        return complexInterfacePlugin.toString();
    }

    /**
     * Init method
     */
    @PluginInit
    private void aInit() {
        LOGGER.info("Init method call : " + this.getClass().getName() + "|active:" + this.isActive + "|coeff:"
                + this.coef);
        // + "|plg_conf:" + this.pluginConfiguration.getId()+ "|plg_int:" + this.complexInterfacePlugin.toString()
    }

}
