/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils;

import fr.cnes.regards.modules.plugins.annotations.PluginInterface;

/**
 * ISamplePlugin
 * 
 * @author Christophe Mertz
 *
 */
@PluginInterface(description = "hello sample plugin interface")
public interface ISamplePlugin {

    /**
     * constant suffix
     */
    public static final String SUFFIXE = "suffix";

    /**
     * constant is active
     */
    public static final String ACTIVE = "isActive";

    /**
     * constant coeff
     */
    public static final String COEFF = "coeff";
    
    /**
     * method echo
     * 
     * @param pMessage
     *            message to display
     * 
     * @return the message
     */
    String echo(String pMessage);

    /**
     * method add
     * 
     * @param pFirst
     *            first element
     * @param pSecond
     *            second item
     * @return the result
     */
    int add(int pFirst, int pSecond);

}
