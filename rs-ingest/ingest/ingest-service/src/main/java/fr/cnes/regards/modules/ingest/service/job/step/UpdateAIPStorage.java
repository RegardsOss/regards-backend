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
package fr.cnes.regards.modules.ingest.service.job.step;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.modules.ingest.domain.job.AIPEntityUpdateWrapper;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPRemoveStorageTask;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.service.aip.utils.IAIPStorageService;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;

/**
 * Update step to remove all {@link OAISDataObjectLocation}s of an {@link AIP} for a given storage identifier.
 *
 * @author Léo Mieulet
 */
public class UpdateAIPStorage implements IUpdateStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAIPStorage.class);

    @Autowired
    private IAIPStorageService aipStorageService;

    @Override
    public AIPEntityUpdateWrapper run(AIPEntityUpdateWrapper aipWrapper, AbstractAIPUpdateTask updateTask)
            throws ModuleException {
        AIPRemoveStorageTask removeStorageTask = (AIPRemoveStorageTask) updateTask;

        if (removeStorageTask.getStorages().containsAll(aipWrapper.getAip().getStorages())) {
            LOGGER.warn("Update tasks are not allowed to delete all location of AIP files. To do so use delete AIP instead.");
        } else {
            // Remove the storage from the AIP and retrieve the list of events to send
            Collection<FileDeletionRequestDTO> deletionRequests = aipStorageService
                    .removeStorages(aipWrapper.getAip(), removeStorageTask.getStorages());

            if (!deletionRequests.isEmpty()) {
                aipWrapper.markAsUpdated(true);
                aipWrapper.addDeletionRequests(deletionRequests);
            }
        }
        return aipWrapper;
    }

}
