/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.bean;

/**
 * @author Christophe Mertz
 *
 */
public interface IPluginUtilsBean {

    <T> void processAutowiredBean(final T pPluginInstance) ;
}
