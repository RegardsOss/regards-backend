/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.modules.plugins.annotations.PluginParameter;

/**
 * SamplePlugin
 * 
 * @author cmertz
 *
 */
@Plugin(author = "CSSI", description = "Sample plugin test", id = "aSamplePlugin", version = "12345-6789-11")
public class SamplePlugin implements ISamplePlugin {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SamplePlugin.class);

    /**
     * A {@link String} parameter
     */
    @PluginParameter(description = "string parameter", name = SUFFIXE)
    private String suffix;

    /**
     * A {@link Integer} parameter
     */
    @PluginParameter(description = "int parameter", name = COEFF)
    private Integer coef;

    /**
     * A {@link Boolean} parameter
     */
    @PluginParameter(description = "boolean parameter", name = ACTIVE)
    private Boolean isActive;

    @Override
    public String echo(final String pMessage) {
        final StringBuffer str = new StringBuffer();
        if (this.isActive) {
            str.append(this.getClass().getName() + " -> " + pMessage + " - " + this.suffix);
        } else {

            str.append(this.getClass().getName() + ":is not active");
        }
        return str.toString();
    }

    @Override
    public int add(final int pFist, final int pSecond) {
        final int res = this.coef * (pFist + pSecond);
        LOGGER.info("add result : " + res);
        return res;
    }

    /**
     * Init method
     */
    @PluginInit
    private void aInit() {
        LOGGER.info("Init method call : " + this.getClass().getName() + "suffixe:" + this.suffix + "|active:"
                + this.isActive + "|coeff:" + this.coef);
    }

}
