/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.bean;

/**
 * @author Christophe Mertz
 *
 */
@FunctionalInterface
public interface IPluginUtilsBean {

    public <T> void processAutowiredBean(final T pPluginInstance) ;
}
