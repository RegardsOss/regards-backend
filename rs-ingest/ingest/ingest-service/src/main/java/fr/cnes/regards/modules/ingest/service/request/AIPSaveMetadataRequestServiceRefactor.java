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
package fr.cnes.regards.modules.ingest.service.request;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.dao.IAIPDumpMetadataRepositoryRefactor;
import fr.cnes.regards.modules.ingest.dao.IAIPSaveMetadataRepositoryRefactor;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPSaveMetadataRequestRefactor;
import fr.cnes.regards.modules.storage.client.RequestInfo;

/**
 * Manage {@link AIPSaveMetadataRequestRefactor} entities
 * @author Iliana Ghazali
 */
@Service
@MultitenantTransactional
public class AIPSaveMetadataRequestServiceRefactor implements IAIPSaveMetadataRequestServiceRefactor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPSaveMetadataRequestServiceRefactor.class);

    @Autowired
    private IAIPSaveMetadataRepositoryRefactor aipStoreMetaDataRepositoryRefactor;

    @Autowired
    private IAIPDumpMetadataRepositoryRefactor aipDumpMetaDataRepositoryRefactor;



    @Override
    public void createRequest() {
        // this method being called from a job, it can be interrupted. to enable the action to be done
        // especially the transaction, we use Thread.interrupted() and not Thread.currentThread().isInterrupted().
        boolean interrupted = Thread.interrupted();
        OffsetDateTime lastDumpDate = aipDumpMetaDataRepositoryRefactor.findLastDumpDate();
        AIPSaveMetadataRequestRefactor.build(lastDumpDate);

        //FIXME keep interrupted ??
        // once the work has been done, we reset the interrupt flag if needed.
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void handleSuccesses(Collection<AIPSaveMetadataRequestRefactor> requests) {
        // Delete the request
        aipStoreMetaDataRepositoryRefactor.deleteAll(requests);
    }

    @Override
    public void handleError(AIPSaveMetadataRequestRefactor request, RequestInfo requestInfo) {
        request.setErrors(requestInfo.getErrorRequests().stream().map(r -> r.getErrorCause())
                .collect(Collectors.toSet()));
        request.setState(InternalRequestState.ERROR);
        aipStoreMetaDataRepositoryRefactor.save(request);
    }

    @Override
    public List<AIPSaveMetadataRequestRefactor> search(List<Long> ids) {
        return aipStoreMetaDataRepositoryRefactor.findAllById(ids);
    }
}
