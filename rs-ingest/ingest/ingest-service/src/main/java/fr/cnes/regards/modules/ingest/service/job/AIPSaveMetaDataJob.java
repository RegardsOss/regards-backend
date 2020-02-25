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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.reflect.TypeToken;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.modules.ingest.dao.IAIPStoreMetaDataRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.request.IAIPStoreMetaDataRequestService;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;

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

    @Autowired
    private IAIPStoreMetaDataRepository storeMetaDataRepository;

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
        LOGGER.debug("[AIP SAVE META JOB] Running job for {} AIPStoreMetaDataRequest(s) requests", requests.size());
        long start = System.currentTimeMillis();
        List<AIPEntity> aipsToUpdate = new ArrayList<>();
        List<FileDeletionRequestDTO> filesToDelete = new ArrayList<>();
        Set<OAISDataObjectLocation> locations = new HashSet<>();
        boolean interrupted = Thread.currentThread().isInterrupted();
        Iterator<AIPStoreMetaDataRequest> requestIter = requests.iterator();
        while (requestIter.hasNext() && !interrupted) {
            AIPStoreMetaDataRequest request = requestIter.next();
            AIPEntity aip = request.getAip();
            String oldChecksum = aip.getChecksum();
            String newChecksum = aip.getChecksum();

            // Check if should recompute checksum
            if (request.isComputeChecksum()) {
                recomputeChecksum(request, aip);
                aipsToUpdate.add(aip);
                newChecksum = aip.getChecksum();
                LOGGER.trace("AIP Manifest checksum updated old : {}, new {}", oldChecksum, newChecksum);
            }

            // Check if there is already existing manifest that should be removed
            if (request.isRemoveCurrentMetaData() && (oldChecksum != null) && !oldChecksum.equals(newChecksum)) {
                locations.addAll(aip.getManifestLocations());
                LOGGER.trace("AIP Manifest to delete on {} locations : {} - {}", aip.getManifestLocations().size(),
                             aip.getAipId(), oldChecksum);
                filesToDelete.addAll(deleteLegacyManifest(oldChecksum, aip.getManifestLocations(), aip.getAipId()));
            }

            advanceCompletion();
            interrupted = Thread.currentThread().isInterrupted();
        }
        // use interrupted() to remove the flag just the time to save handle state
        interrupted = Thread.interrupted();
        if (interrupted) {
            requests.forEach(r -> r.setState(InternalRequestState.ABORTED));
            storeMetaDataRepository.saveAll(requests);
            Thread.currentThread().interrupt();
        } else {
            aipSaveMetaDataService.handle(requests, aipsToUpdate, filesToDelete);
            LOGGER.info(this.getClass().getSimpleName()
                    + ": {} manifests updated, {} manifests deleted on {} locations.", aipsToUpdate.size(),
                        filesToDelete.size(), locations.size());
        }
        LOGGER.debug("[AIP SAVE META JOB] Job handled for {} AIPStoreMetaDataRequest(s) requests in {}ms",
                     requests.size(), System.currentTimeMillis() - start);
    }

    private void recomputeChecksum(AIPStoreMetaDataRequest request, AIPEntity aip) {
        try {
            aipService.computeAndSaveChecksum(aip);
        } catch (ModuleException e) {
            request.addError(e.getMessage());
            request.setState(InternalRequestState.ERROR);
        }
    }

    private List<FileDeletionRequestDTO> deleteLegacyManifest(String manifestChecksum,
            Collection<OAISDataObjectLocation> manifestLocations, String aipId) {
        List<FileDeletionRequestDTO> filesToDelete = new ArrayList<>();
        // Add the AIP itself (on each storage) to the file list to remove
        for (OAISDataObjectLocation location : manifestLocations) {
            filesToDelete.add(FileDeletionRequestDTO.build(manifestChecksum, location.getStorage(), aipId, false));
        }
        return filesToDelete;
    }

    @Override
    public int getCompletionCount() {
        return requests.size();
    }

}