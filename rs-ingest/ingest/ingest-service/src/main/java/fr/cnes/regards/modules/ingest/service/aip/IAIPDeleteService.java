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
package fr.cnes.regards.modules.ingest.service.aip;

import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;

/**
 * AIP Service interface. Service to handle business around {@link AIPEntity}s
 *
 * @author Sébastien Binda
 */
public interface IAIPDeleteService {

    /**
     * Send a group of event to remove all referenced files, including manifests, that any {@link AIPEntity}
     * linked to the provided {@link SIPEntity#getSipId()}
     * Update the provided request in the same transaction
     */
    void scheduleLinkedFilesDeletion(OAISDeletionRequest request);

    /**
     * Remove all {@link AIPEntity} linked to an {@link SIPEntity#getSipId()}
     */
    void processDeletion(String sipId, boolean deleteIrrevocably);

    /**
     * Check if a deletion request is running or pending  for the given aip
     *
     * @param aip
     * @return [TRUE|FALSE]
     */
    boolean deletionAlreadyPending(AIPEntity aip);

}
