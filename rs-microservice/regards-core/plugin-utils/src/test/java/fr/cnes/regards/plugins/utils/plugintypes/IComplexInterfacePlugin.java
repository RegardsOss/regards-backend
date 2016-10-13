/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.plugintypes;

import fr.cnes.regards.modules.plugins.annotations.PluginInterface;

/**
 * IComplexInterfacePlugin
 * 
 * @author cmertz
 *
 */
@PluginInterface(description = "hello plugin interface")
public interface IComplexInterfacePlugin {

    /**
     * constant klong
     */
    public static final String LONG_PARAM = "long_param";

    /**
     * method mult
     * 
     * @param pFirst
     *            first parameter's method
     * @param pSecond
     *            second parameter's method
     * @return the result
     */
    public int mult(int pFirst, int pSecond);

}
