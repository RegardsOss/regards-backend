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
 * ISamplePlugin
 * 
 * @author cmertz
 *
 */
@Plugin(author = "CSSI", description = "Sample plugin test", id = "anErrorPluginInterface", version = "0.0.1")
public class ErrorInterfacePlugin implements IComplexInterfacePlugin {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorInterfacePlugin.class);

    /**
     * A {@link Long} parameter
     */
    @PluginParameter(description = "long parameter", name = LONG_PARAM)
    private Long aLong;

    /**
     * Init method
     */
    @PluginInit
    private void aInit() {
        LOGGER.info("Init method call : " + this.getClass().getName() + "|long:" + this.aLong);
    }

    @Override
    public int mult(final int pFirst, final int pSecond) {
        return 0;
    }

}
