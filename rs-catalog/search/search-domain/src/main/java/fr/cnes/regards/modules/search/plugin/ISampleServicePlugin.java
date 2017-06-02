/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.plugin;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;

/**
 * ISampleServicePlugin
 * 
 * @author Christophe Mertz
 *
 */
@PluginInterface(description = "hello sample plugin interface")
public interface ISampleServicePlugin extends IService {

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
