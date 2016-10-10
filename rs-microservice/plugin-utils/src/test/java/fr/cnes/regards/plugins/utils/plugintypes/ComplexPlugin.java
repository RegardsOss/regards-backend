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
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ComplexPlugin.class);

    /**
     * A {@link String} parameter
     */
    @PluginParameter(description = "string parameter", name = PLG)
    private ISampleInterfacePlugin sampleInterfacePlugin;
    // ou directement l'implémentation

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
            str.append(this.getClass().getName() + "-" + pMessage + this.sampleInterfacePlugin.toString());
        } else {

            str.append(this.getClass().getName() + ":is not active");
        }
        return str.toString();
    }

    @Override
    public int add(int pFist, int pSecond) {
        final float f = sampleInterfacePlugin.mult(1, 5);
        LOGGER.info("float=" + f);
        final int res = this.coef * (pFist + pSecond);
        LOGGER.info(this.getClass().getName() + ":" + res);
        return res;
    }

    /**
     * Init method
     */
    @PluginInit
    private void aInit() {
        LOGGER.info("Init method call : " + this.getClass().getName() + "|active:" + this.isActive + "|coeff:"
                + this.coef);
    }

}
