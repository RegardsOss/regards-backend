/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * SampleErrorPlugin
 *
 * @author Christophe Mertz
 */
@Plugin(description = "Sample plugin test", id = "aSampleErrorPlugin", version = "0.0.1", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class SampleErrorPlugin implements ISamplePlugin {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleErrorPlugin.class);

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
        if (isActive) {
            str.append(this.getClass().getName() + " -> " + pMessage + suffix);
        } else {

            str.append(this.getClass().getName() + ":is not active");
        }
        return str.toString();
    }

    @Override
    public int add(final int pFist, final int pSecond) {
        final int res = coef * (pFist + pSecond);
        LOGGER.info("add result : " + res);
        return res;
    }

    /**
     * Init method
     */
    @PluginInit
    private void aInit() {
        LOGGER.info("Init method call : " + this.getClass().getName() + "suffixe:" + suffix + "|active:" + isActive
                + "|coeff:" + coef);
        throw new IllegalArgumentException();
    }

}
