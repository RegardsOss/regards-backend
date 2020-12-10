/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.processing.dto.ProcessLabelDTO;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.dto.ProcessesByDatasetsDTO;
import fr.cnes.regards.modules.processing.service.ProcessPluginConfigService.DeleteAttemptOnUsedProcessException;

/**
 * This interface defines signatures to interact with {@link fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration}.
 *
 * @author gandrieu
 */
public interface IProcessPluginConfigService {

    Collection<ProcessPluginConfigurationRightsDTO> findAllRightsPluginConfigs();

    ProcessPluginConfigurationRightsDTO findByBusinessId(UUID processBusinessId);

    ProcessPluginConfigurationRightsDTO update(UUID processBusinessId, ProcessPluginConfigurationRightsDTO rightsDto)
            throws ModuleException;

    ProcessPluginConfigurationRightsDTO create(ProcessPluginConfigurationRightsDTO rightsDto)
            throws EntityNotFoundException;

    Boolean canDelete(UUID processBusinessId);

    ProcessPluginConfigurationRightsDTO delete(UUID processBusinessId)
            throws DeleteAttemptOnUsedProcessException, ModuleException;

    void putDatasetLinkedProcesses(List<UUID> processBusinessIds, String dataset);

    Collection<ProcessLabelDTO> getDatasetLinkedProcesses(String dataset);

    ProcessesByDatasetsDTO findProcessesByDatasets(List<String> datasets);

    void attachRoleToProcess(UUID processBusinessId, String userRole);
}
