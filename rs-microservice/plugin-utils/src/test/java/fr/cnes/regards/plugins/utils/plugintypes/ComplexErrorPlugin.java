/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.plugintypes;

import fr.cnes.regards.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.plugins.utils.ISamplePlugin;

/**
 * ISamplePlugin
 * 
 * @author cmertz
 *
 */
@Plugin(author = "CSSI", description = "Complex plugin test", id = "aComplexErrorPlugin", version = "0.0.1")
public class ComplexErrorPlugin implements ISamplePlugin {

    /**
     * A {@link String} parameter
     */
    @PluginParameter(description = "plugin parameter", name = PLG)
    private INotInterfacePlugin interfacePlugin;

    /**
     * A {@link Integer} parameter
     */
    @PluginParameter(description = "int parameter", name = COEFF)
    private Integer coef = 0;

    @Override
    public String echo(String pMessage) {
        final StringBuffer str = new StringBuffer();
        str.append(this.getClass().getName() + "-" + pMessage + this.interfacePlugin.toString());
        return str.toString();
    }

    @Override
    public int add(int pFist, int pSecond) {
        return 0;
    }

}
