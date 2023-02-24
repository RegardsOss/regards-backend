/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.catalog.services.domain.ServicePluginParameters;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import fr.cnes.regards.modules.catalog.services.domain.plugins.IService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Defines operations on rs-catalog's plugins.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Xavier-Alexandre Brochard
 */
public interface IServiceManager {

    /**
     * Retrieve all PluginConfiguration in the system for plugins of type {@link IService} linked to a dataset, and adds applicationModes
     * & entityTypes info via a DTO
     *
     * @param pDatasetIds    Ids of dataset. Can be <code>empty</code>.
     * @param pServiceScopes scopes we are interrested in. Can be <code>empty</code>.
     * @return PluginConfigurations in the system for plugins of type {@link IService} linked to a dataset for a given
     * scope. Returns an empty list if <code>pDatasetId</code> is <code>null</code>
     */
    List<PluginConfigurationDto> retrieveServices(List<String> pDatasetIds, List<ServiceScope> pServiceScopes);

    /**
     * Apply the service
     *
     * @param pluginConfigurationBusinessId Plugin configuration to run
     * @param pServicePluginParameters      Plugin parameters
     * @return the result of the service call wrapped in a resonse entity
     */
    ResponseEntity<StreamingResponseBody> apply(final String pluginConfigurationBusinessId,
                                                final ServicePluginParameters pServicePluginParameters,
                                                HttpServletResponse response) throws ModuleException;

}