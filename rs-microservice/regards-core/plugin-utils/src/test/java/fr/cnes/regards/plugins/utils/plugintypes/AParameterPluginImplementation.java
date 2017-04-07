/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.plugintypes;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;

/**
 * ParameterPlugin
 *
 * @author Christophe Mertz
 */
@Plugin(description = "Parameter plugin test", id = "aParameterPlugin", version = "0.0.1", author = "REGARDS Team",
        contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI", url = "https://github.com/RegardsOss")
public class AParameterPluginImplementation implements IComplexInterfacePlugin {

    /**
     * A {@link Integer} parameter
     */
    @PluginParameter(description = "long parameter", name = LONG_PARAM)
    private Long ll;

    @Override
    public int mult(final int pFirst, final int pSecond) {
        final int res = ll.intValue() * (pFirst * pSecond);

        return res;
    }

}
