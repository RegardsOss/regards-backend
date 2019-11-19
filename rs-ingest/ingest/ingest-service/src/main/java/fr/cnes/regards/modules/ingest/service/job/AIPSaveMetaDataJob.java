/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.job;

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.request.IAIPStoreMetaDataRequestService;
import fr.cnes.regards.modules.storagelight.domain.dto.request.FileDeletionRequestDTO;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Léo Mieulet
 */
public class AIPSaveMetaDataJob extends AbstractJob<Void> {

    public static final String UPDATE_METADATA_REQUEST_IDS = "UPDATE_METADATA_REQUEST_IDS";

    private List<AIPStoreMetaDataRequest> requests;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private IAIPStoreMetaDataRequestService aipSaveMetaDataService;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        // Retrieve param
        Type type = new TypeToken<List<Long>>() {
        }.getType();
        List<Long> updateRequestIds = getValue(parameters, UPDATE_METADATA_REQUEST_IDS, type);
        // Retrieve list of AIP save metadata requests to handle
        requests = aipSaveMetaDataService.search(updateRequestIds);
    }

    @Override
    public void run() {
        List<AIPEntity> aipsToStore = new ArrayList<>();
        List<AIPEntity> aipsToUpdate = new ArrayList<>();
        List<FileDeletionRequestDTO> filesToDelete = new ArrayList<>();
        for (AIPStoreMetaDataRequest request : requests) {
            AIPEntity aip = request.getAip();
            // Check if there is already existing manifest that should be removed
            if (request.isRemoveCurrentMetaData()) {
                filesToDelete.addAll(deleteLegacyManifest(aip));
            }
            // Check if should recompute checksum
            if (request.isComputeChecksum()) {
                recomputeChecksum(request, aip);
                aipsToUpdate.add(aip);
            }
            // If everything is still ok, add the AIP to the list of storable aips
            if (request.getState() != InternalRequestStep.ERROR) {
                aipsToStore.add(aip);
            }
            advanceCompletion();
        }
        aipSaveMetaDataService.handle(requests, aipsToStore, aipsToUpdate, filesToDelete);
    }

    private void recomputeChecksum(AIPStoreMetaDataRequest request, AIPEntity aip) {
        try {
            aipService.computeAndSaveChecksum(aip);
        } catch (ModuleException e) {
            request.addError(e.getMessage());
            request.setState(InternalRequestStep.ERROR);
        }
    }

    private List<FileDeletionRequestDTO> deleteLegacyManifest(AIPEntity aip) {
        List<FileDeletionRequestDTO> filesToDelete = new ArrayList<>();
        // Add the AIP itself (on each storage) to the file list to remove
        for (OAISDataObjectLocation location : aip.getManifestLocations()) {
            filesToDelete.add(FileDeletionRequestDTO.build(aip.getChecksum(), location.getStorage(),
                    aip.getAipId(), false));
        }
        return filesToDelete;
    }

    @Override
    public int getCompletionCount() {
        return requests.size();
    }

}