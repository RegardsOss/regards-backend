/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.aip;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.fileaccess.dto.request.FileDeletionDto;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;

import java.util.Collection;
import java.util.List;

/**
 * Manage AIP storage
 *
 * @author Léo Mieulet
 */
public interface IAIPStorageService {

    /**
     * Store or reference AIPs Files with taking into account of retry action by user
     *
     * @return file storage event group_id list
     */
    List<String> storeAIPFiles(IngestRequest request) throws ModuleException;

    /**
     * Update provided {@link AIPEntity} aips content info with files metadata
     *
     * @param aips              to update
     * @param storeRequestInfos storage events
     */
    void updateAIPsContentInfosAndLocations(List<AIPEntity> aips, Collection<RequestResultInfoDto> storeRequestInfos);

    /**
     * Update provided {@link AIPEntity} aip with a list of new file storage locations
     *
     * @param aip               to update
     * @param storeRequestInfos storage events
     * @return true when aip have been impacted by these events
     */
    AIPUpdateResult addAIPLocations(AIPEntity aip, Collection<RequestResultInfoDto> storeRequestInfos);

    /**
     * Update provided {@link AIPEntity} aip with a list of removed file storage locations
     *
     * @param aip               to update
     * @param storeRequestInfos storage events
     * @return true when aip have been impacted by these events
     */
    AIPUpdateResult removeAIPLocations(AIPEntity aip, Collection<RequestResultInfoDto> storeRequestInfos);

    /**
     * Remove a list of storage id from the AIP and retrieve the list of events to send
     *
     * @param removedStorages list of storage metadata that will be removed from the AIP
     * @return the list of events to sent to storage, empty if nothing have been done
     */
    Collection<FileDeletionDto> removeStorages(AIPEntity aip, List<String> removedStorages);
}
