/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.plugintypes;

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
@Plugin(author = "CSSI", description = "Sample plugin test", id = "aSamplePluginInterface", version = "0.0.1")
public class SampleInterfacePlugin implements ISampleInterfacePlugin {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleInterfacePlugin.class);

    /**
     * A {@link Byte} parameter
     */
    @PluginParameter(description = "kbyte parameter", name = KBYTE)
    private Byte aByte;

    /**
     * A {@link Float} parameter
     */
    @PluginParameter(description = "float parameter", name = KFLOAT)
    private Float aFloat;

    /**
     * A {@link Long} parameter
     */
    @PluginParameter(description = "long parameter", name = KLONG)
    private Long aLong;

    /**
     * A {@link Short} parameter
     */
    @PluginParameter(description = "long parameter", name = KSHORT)
    private Short aShort;

    /**
     * Init method
     */
    @PluginInit
    private void aInit() {
        LOGGER.info("Init method call : " + this.getClass().getName() + "byte:" + this.aByte + "|float:" + this.aFloat
                + "|long:" + this.aLong + "|short:" + this.aShort);
    }

    @Override
    public int mult(int pFist, int pSecond) {
        return this.aShort * (pFist + pSecond);
    }

}
