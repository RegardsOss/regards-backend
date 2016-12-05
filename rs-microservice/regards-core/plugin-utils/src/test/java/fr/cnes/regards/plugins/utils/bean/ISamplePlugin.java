/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.bean;

import fr.cnes.regards.modules.plugins.annotations.PluginInterface;

/**
 * ISamplePlugin
 * 
 * @author Christophe Mertz
 *
 */
@PluginInterface(description="hello")
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
