/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service;

import org.springframework.data.domain.Page;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;

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

    /**
     * allow the caller to know if the given Dataset can be treated by this implementation
     *
     * @param pCandidate
     * @return if this implementation can be applied to the given dataset
     */
    public boolean isRelevant(Dataset pCandidate);

}
