/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.entities.domain.Dataset;

/**
 *
 * TODO: implement and see what the interface shoudl define
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@PluginInterface(description = "TODO")
public interface IService {

    /**
     * allow the caller to know if the given Dataset can be treated by this implementation
     * 
     * @param pCandidate
     * @return if this implementation can be applied to the given dataset
     */
    public boolean isRelevant(Dataset pCandidate);

}
