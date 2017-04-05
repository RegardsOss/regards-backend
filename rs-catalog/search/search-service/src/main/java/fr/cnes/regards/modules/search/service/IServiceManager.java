/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service;

import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.search.domain.IService;
import fr.cnes.regards.modules.search.domain.ServiceScope;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public interface IServiceManager {

    /**
     * retrieve all PluginConfiguration in the system for plugins of type {@link IService} linked to a dataset for a
     * given scope
     *
     * @param pServiceScope
     *            scope we are interrested in
     * @param pDatasetId
     *            id of dataset
     *
     * @return PluginConfigurations in the system for plugins of type {@link IService} linked to a dataset for a given
     *         scope
     * @throws EntityNotFoundException
     *             thrown is the pDatasetId does not represent any Dataset.
     */
    Set<PluginConfiguration> retrieveServices(Long pDatasetId, ServiceScope pServiceScope)
            throws EntityNotFoundException;

    /**
     * Apply the service
     * @param pDatasetId
     * @param pServiceName
     * @param pDynamicParameters
     * @return the result of the service call wrapped in a resonse entity
     * @throws ModuleException
     */
    ResponseEntity<?> apply(Long pDatasetId, String pServiceName, Map<String, String> pDynamicParameters)
            throws ModuleException;

}