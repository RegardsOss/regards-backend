/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.plugintypes;

import fr.cnes.regards.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.plugins.annotations.PluginParameter;

/**
 * ParameterPlugin
 * 
 * @author cmertz
 *
 */
@Plugin(author = "CSSI", description = "Parameter plugin test", id = "aParameterPlugin", version = "0.0.1")
public class ParameterPlugin implements IComplexInterfacePlugin {

    /**
     * A {@link Integer} parameter
     */
    @PluginParameter(description = "long parameter", name = LONG_PARAM)
    private Long ll;

    @Override
    public int mult(int pFirst, int pSecond) {
        final int res = ll.intValue() * (pFirst * pSecond);

        return res;
    }

}
