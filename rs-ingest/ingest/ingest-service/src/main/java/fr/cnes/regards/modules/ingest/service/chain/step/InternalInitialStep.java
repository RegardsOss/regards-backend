/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.chain.step;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;

/**
 * Initialize {@link SIPEntity} from specified {@link IngestRequest}
 *
 * @author Marc SORDI
 *
 */
public class InternalInitialStep extends AbstractIngestStep<IngestRequest, SIPEntity> {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISIPService sipService;

    public InternalInitialStep(IngestProcessingJob job, IngestProcessingChain ingestChain) {
        super(job, ingestChain);
    }

    @Override
    protected SIPEntity doExecute(IngestRequest request) throws ProcessingStepException {
        job.getCurrentRequest().setStep(IngestRequestStep.LOCAL_INIT);

        SIP sip = request.getSip();

        // Compute checksum
        String checksum;
        try {
            checksum = sipService.calculateChecksum(sip);
        } catch (NoSuchAlgorithmException | IOException e) {
            String error = String.format("Cannot compute checksum for SIP identified by %s", sip.getId());
            addError(error);
            throw new ProcessingStepException(error, e);
        }

        // Is SIP already ingested?
        if (sipService.isAlreadyIngested(checksum)) {
            String error = String.format("SIP \"%s\" already submitted", sip.getId());
            addError(error);
            throw new ProcessingStepException(error);
        }

        // Manage version
        Integer version = sipService.getNextVersion(sip);
        SIPEntity entity = SIPEntity.build(runtimeTenantResolver.getTenant(), request.getMetadata(), sip, version,
                                           SIPState.INGESTED);
        entity.setChecksum(checksum);
        return entity;
    }

    @Override
    protected void doAfterError(IngestRequest request) {
        handleRequestError(String.format("Internal SIP creation from external SIP \"%s\" fails",
                                         request.getSip().getId()));
    }

}
