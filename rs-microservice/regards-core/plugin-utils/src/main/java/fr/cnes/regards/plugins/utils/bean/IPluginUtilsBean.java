/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.bean;

import fr.cnes.regards.plugins.utils.PluginUtilsException;

/**
 * @author Christophe Mertz
 *
 */
@FunctionalInterface
public interface IPluginUtilsBean {

    public <T> void processAutowiredBean(final T pPluginInstance) throws PluginUtilsException;
}
