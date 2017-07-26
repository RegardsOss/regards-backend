/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.catalog.services.service;

import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.catalog.services.domain.IService;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public interface IServiceManager {

    /**
     * Retrieve all PluginConfiguration in the system for plugins of type {@link IService} linked to a dataset for a
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
    Set<PluginConfiguration> retrieveServices(String pDatasetId, ServiceScope pServiceScope)
            throws EntityNotFoundException;

    /**
     * Retrieve all PluginConfiguration in the system for plugins of type {@link IService} linked to a dataset, and adds applicationModes
     * & entityTypes info via a DTO
     *
     * @param pDatasetId
     *            id of dataset
     * @return PluginConfigurations in the system for plugins of type {@link IService} linked to a dataset for a given
     *         scope
     * @throws EntityNotFoundException
     *             thrown is the pDatasetId does not represent any Dataset.
     */
    Set<PluginConfigurationDto> retrieveServicesWithMeta(String pDatasetId) throws EntityNotFoundException;

    /**
     * Apply the service
     *
     * @param pDatasetId
     * @param pServiceName
     * @param pDynamicParameters
     * @return the result of the service call wrapped in a resonse entity
     * @throws ModuleException
     */
    ResponseEntity<?> apply(String pDatasetId, String pServiceName, Map<String, String> pDynamicParameters)
            throws ModuleException;

}