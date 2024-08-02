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
package fr.cnes.regards.modules.ingest.service.chain.step;

import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.IngestErrorType;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import fr.cnes.regards.modules.ingest.dto.VersioningMode;
import fr.cnes.regards.modules.ingest.service.chain.step.info.ErrorModeHandling;
import fr.cnes.regards.modules.ingest.service.chain.step.info.StepErrorInfo;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import fr.cnes.regards.modules.ingest.service.request.RequestDeletionService;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Initialize {@link SIPEntity} from specified {@link IngestRequest}
 *
 * @author Marc SORDI
 */
public class InternalInitialStep extends AbstractIngestStep<IngestRequest, SIPEntity> {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISIPService sipService;

    @Autowired
    private RequestDeletionService requestDeletionService;

    public InternalInitialStep(IngestProcessingJob job, IngestProcessingChain ingestChain) {
        super(job, ingestChain);
    }

    @Override
    protected SIPEntity doExecute(IngestRequest request) throws ProcessingStepException {
        job.getCurrentRequest().setStep(IngestRequestStep.LOCAL_INIT);

        SIPDto sip = request.getSip();

        //remove null tags because they have no use!
        sip.getTags().remove(null);

        // If replace error mode is activated, first delete old request/sip/aip in error state for the same providerId
        if (request.getMetadata().getReplaceErrors()) {
            requestDeletionService.deleteRequests(ingestRequestService.findErrorRequestsByProviderId(request.getProviderId()));
        }

        // Compute checksum
        String checksum;
        try {
            checksum = sipService.calculateChecksum(sip);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw throwProcessingStepException(IngestErrorType.INITIAL_CHECKSUM,
                                               String.format("Cannot compute checksum for SIP identified by %s",
                                                             sip.getId()),
                                               e);
        }

        // Is SIP already ingested?
        if (sipService.isAlreadyIngested(checksum)) {
            throw throwProcessingStepException(IngestErrorType.INITIAL_SIP_ALREADY_EXISTS,
                                               String.format("The SIP \"%s\" already exists and there is no difference "
                                                             + "between this one and the stored one.", sip.getId()));
        }

        // Manage version
        Integer version = sipService.getNextVersion(sip);
        // handle versioning mode
        VersioningMode versioningMode = request.getMetadata().getVersioningMode();
        switch (versioningMode) {
            case IGNORE:
                // In this case, lets break generation, only if it is not the first one, with proper message
                if (version != 1) {
                    ingestRequestService.ignore(request);
                    throw new ProcessingStepException(IngestErrorType.INITIAL_NOT_FIRST_VERSION_IGNORE,
                                                      String.format(
                                                          "Sip %s is not generated because this is not the first version "
                                                          + "and versioning mode ask to ignore this one.",
                                                          sip.getId()));
                }
                break;
            case MANUAL:
                // In this case, lets break generation, only if it is not the first one, with proper message
                if (version != 1) {
                    ingestRequestService.waitVersioningMode(request);
                    throw new ProcessingStepException(IngestErrorType.INITIAL_NOT_FIRST_VERSION_MANUAL,
                                                      String.format(
                                                          "Sip %s is not generated because this is not the first version "
                                                          + "and versioning mode ask for manual decision.",
                                                          sip.getId()));
                }
                break;
            case INC_VERSION, REPLACE:
                // in these cases, there is nothing to do right now
                break;
            default:
                throw throwProcessingStepException(IngestErrorType.INITIAL_UNKNOWN_VERSIONING,
                                                   String.format(
                                                       "This versioning mode is not recognized by the system: %s",
                                                       versioningMode));
        }

        SIPEntity entity = SIPEntity.build(runtimeTenantResolver.getTenant(),
                                           request.getMetadata(),
                                           sip,
                                           version,
                                           SIPState.INGESTED);
        entity.setChecksum(checksum);
        return entity;
    }

    @Override
    protected StepErrorInfo getStepErrorInfo(IngestRequest request, Exception exception) {
        String stepName = "INITIAL";
        if ((request.getState() == InternalRequestState.WAITING_VERSIONING_MODE) || (request.getState()
                                                                                     == InternalRequestState.IGNORED)) {
            return new StepErrorInfo(stepName,
                                     ErrorModeHandling.NOTHING_TO_DO,
                                     "waiting for admin action.",
                                     IngestErrorType.INITIAL);
        }
        return buildDefaultStepErrorInfo(stepName,
                                         exception,
                                         String.format("Internal SIP creation from external SIP \"%s\" fails.",
                                                       request.getSip().getId()),
                                         IngestErrorType.INITIAL);
    }
}
