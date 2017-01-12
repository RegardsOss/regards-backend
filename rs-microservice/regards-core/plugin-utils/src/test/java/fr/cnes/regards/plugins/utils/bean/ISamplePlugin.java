/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.bean;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;

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
     * method echo
     * 
     * @param pMessage
     *            message to display
     * 
     * @return the message
     */
    String echo(String pMessage);

}
