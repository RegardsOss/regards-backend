/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.domain;


/**
 *
 * Interface for plugin types enum.
 *
 * @author sbinda
 */
public interface IPluginType {

    /**
     * Get method.
     *
     * @return the classType
     */
    Class<?> getClassType();

    /**
     * Get method.
     *
     * @return the name
     */
    String getName();
}
