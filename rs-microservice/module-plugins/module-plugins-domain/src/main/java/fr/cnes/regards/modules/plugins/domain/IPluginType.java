/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.plugins.domain;


/**
 *
 * Interface for plugin types enum.
 *
 * @author cmertz
 */
public interface IPluginType {

    Class<?> getClassType();

    String getName();
}
