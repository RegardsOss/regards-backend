/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.dump.DumpService;
import fr.cnes.regards.framework.dump.ObjectDump;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.ingest.dao.IAIPSaveMetadataRepositoryRefactor;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPSaveMetadataRequestRefactor;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.ingest.service.aip.IAIPMetadataServiceRefactor;
import fr.cnes.regards.modules.ingest.service.request.AIPSaveMetadataRequestServiceRefactor;
import fr.cnes.regards.modules.ingest.service.request.IAIPSaveMetadataRequestServiceRefactor;
import fr.cnes.regards.modules.ingest.service.request.IAIPStoreMetaDataRequestService;

/**
 * @author Léo Mieulet
 */
public class AIPSaveMetadataJobRefactor extends AbstractJob<Void> {

    public static final String SAVE_METADATA_REQUESTS_IDS = "SAVE_METADATA_REQUESTS_IDS";

    private List<AIPSaveMetadataRequestRefactor> requests;

    private Set<Long> updatedAipIds;

    @Autowired
    private IAIPSaveMetadataRequestServiceRefactor aipSaveMetadataRequestServiceRefactor;

    @Autowired
    private IAIPSaveMetadataRepositoryRefactor aipSaveMetadataRepositoryRefactor;

    @Autowired
    private IAIPMetadataServiceRefactor aipMetadataServiceRefactor;


    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        // Retrieve param
        Type type = new TypeToken<List<Long>>() {

        }.getType();
        List<Long> saveMetadataRequestIds = getValue(parameters, SAVE_METADATA_REQUESTS_IDS, type);
        requests = aipSaveMetadataRequestServiceRefactor.search(saveMetadataRequestIds);
    }

    @Override
    public void run() {
        logger.debug("[AIP SAVE METADATA JOB] Running job for {} AIPStoreMetaDataRequest(s) requests", requests.size());
        long start = System.currentTimeMillis();
        boolean interrupted = Thread.currentThread().isInterrupted();
        Iterator<AIPSaveMetadataRequestRefactor> requestIter = requests.iterator();
        while (requestIter.hasNext() && !interrupted) {
            AIPSaveMetadataRequestRefactor request = requestIter.next();
            aipMetadataServiceRefactor.dumpObject(request);
            advanceCompletion();
            interrupted = Thread.currentThread().isInterrupted();
        }

        // use interrupted() to remove the flag just the time to save handle state
        interrupted = Thread.interrupted();
        if (interrupted) {
            requests.forEach(r -> r.setState(
                    InternalRequestState.ABORTED)); //TODO : check if all req should be put to ABORTED (or only those not processed ??)
            aipSaveMetadataRepositoryRefactor.saveAll(requests);
            Thread.currentThread().interrupt();
        }
        logger.debug("[AIP SAVE META JOB] Job handled for {} AIPSaveMetaDataRequest(s) requests in {}ms",
                     requests.size(), System.currentTimeMillis() - start);
    }

}
