/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.domain;

import java.util.Set;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;

/**
 *
 * Plugin applying processus on a {@link Dataset} or one of its subset
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@PluginInterface(description = "Plugin applying processus on a {@link Dataset} or one of its subset")
public interface IService {

    /**
     *
     * apply the processus described by this instance of IService
     *
     * @param pQuery
     *            request string to be interpreted
     * @return processed results
     */
    public Set<DataObject> apply(String pQuery);

}
