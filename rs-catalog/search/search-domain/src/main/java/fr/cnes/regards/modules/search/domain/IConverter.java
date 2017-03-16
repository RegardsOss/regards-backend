/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.domain;

import org.springframework.data.domain.Page;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.entities.domain.DataObject;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@PluginInterface(description = "plugin interface for converter plugins")
public interface IConverter {

    /**
     * Convert a set of DataObject according to the plugin parameter
     *
     * @param dataObjects
     *            page of object to convert
     * @return converted page
     */
    public Page<DataObject> convert(Page<DataObject> dataObjects);

}
