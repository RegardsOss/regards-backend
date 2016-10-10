/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.plugintypes;

import fr.cnes.regards.modules.plugins.annotations.PluginInterface;

/**
 * ISampleInterfacePlugin
 * 
 * @author cmertz
 *
 */
@PluginInterface(description = "hello plugin interface")
public interface ISampleInterfacePlugin {

    /**
     * constant klong
     */
    public static final String KLONG = "klong";

    /**
     * constant kshort
     */
    public static final String KSHORT = "kshort";

    /**
     * constant kbyte
     */
    public static final String KBYTE = "kbyte";

    /**
     * constant kfloat
     */
    public static final String KFLOAT = "kfloat";


    /**
     * method mult
     * 
     * @param pFist
     *            first element
     * @param pSecond
     *            second item
     * @return the result
     */
    public int mult(int pFist, int pSecond);

}
