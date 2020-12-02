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

import java.util.List;
import java.util.UUID;

import fr.cnes.regards.modules.processing.dto.ProcessLabelDTO;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.dto.ProcessesByDatasetsDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * TODO : Class description
 *
 * @author Guillaume Andrieu
 *
 */
public interface IProcessPluginConfigService {

    Flux<ProcessPluginConfigurationRightsDTO> findAllRightsPluginConfigs();

    Mono<ProcessPluginConfigurationRightsDTO> findByBusinessId(UUID processBusinessId);

    Mono<ProcessPluginConfigurationRightsDTO> update(String tenant, UUID processBusinessId,
            ProcessPluginConfigurationRightsDTO rightsDto);

    Mono<ProcessPluginConfigurationRightsDTO> create(String tenant, ProcessPluginConfigurationRightsDTO rightsDto);

    Mono<Boolean> canDelete(UUID processBusinessId);

    Mono<ProcessPluginConfigurationRightsDTO> delete(UUID processBusinessId, String tenant);

    Mono<Void> putDatasetLinkedProcesses(List<UUID> processBusinessIds, String dataset);

    Flux<ProcessLabelDTO> getDatasetLinkedProcesses(String dataset);

    Mono<ProcessesByDatasetsDTO> findProcessesByDatasets(List<String> datasets);

    Mono<Void> attachRoleToProcess(UUID processBusinessId, String userRole);
}
