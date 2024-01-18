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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.dto.OAISDataObjectDto;
import fr.cnes.regards.framework.oais.dto.OAISDataObjectLocationDto;
import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import fr.cnes.regards.modules.ingest.domain.job.AIPEntityUpdateWrapper;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateFileLocationTask;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateTaskType;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import fr.cnes.regards.modules.ingest.service.aip.AIPUpdateResult;
import fr.cnes.regards.modules.ingest.service.aip.IAIPStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Update step to add/remove a {@link OAISDataObjectLocationDto} to an {@link AIPDto} {@link OAISDataObjectDto}
 *
 * @author Léo Mieulet
 */
public class UpdateAIPLocation implements IUpdateStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAIPLocation.class);

    @Autowired
    private IAIPStorageService aipStorageService;

    @Override
    public AIPEntityUpdateWrapper run(AIPEntityUpdateWrapper aipWrapper, AbstractAIPUpdateTask updateTask)
        throws ModuleException {

        AIPUpdateTaskType taskType = updateTask.getType();
        AIPUpdateFileLocationTask updateFileLocation = (AIPUpdateFileLocationTask) updateTask;
        List<RequestResultInfoDto> fileLocationUpdates = updateFileLocation.getFileLocationUpdates();
        AIPUpdateResult updated;
        if (taskType == AIPUpdateTaskType.ADD_FILE_LOCATION) {
            updated = aipStorageService.addAIPLocations(aipWrapper.getAip(), fileLocationUpdates);
        } else {
            updated = aipStorageService.removeAIPLocations(aipWrapper.getAip(),
                                                           updateFileLocation.getFileLocationUpdates());
        }
        LOGGER.debug("[AIP {}] Running update task {}. updated={} manifestUpdate={}",
                     aipWrapper.getAip().getAipId(),
                     taskType.toString(),
                     updated.isAipEntityUpdated(),
                     updated.isAipUpdated());
        if (updated.isAipEntityUpdated()) {
            aipWrapper.markAsUpdated(updated.isAipUpdated());
        }
        return aipWrapper;
    }
}
