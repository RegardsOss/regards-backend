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

import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.IngestErrorType;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.service.chain.step.info.StepErrorInfo;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;

import java.util.List;

/**
 * Persist all generated entities in database : {@link SIPEntity} including {@link SIPDto} and {@link AIPEntity} including {@link AIPDto}(s)
 *
 * @author Marc SORDI
 */
public class InternalFinalStep extends AbstractIngestStep<List<AIPDto>, List<AIPEntity>> {

    public InternalFinalStep(IngestProcessingJob job, IngestProcessingChain ingestChain) {
        super(job, ingestChain);
    }

    @Override
    protected List<AIPEntity> doExecute(List<AIPDto> aips) throws ProcessingStepException {
        job.getCurrentRequest().setStep(IngestRequestStep.LOCAL_FINAL);
        return ingestRequestService.handleIngestJobSucceed(job.getCurrentRequest(), job.getCurrentEntity(), aips);
    }

    @Override
    protected StepErrorInfo getStepErrorInfo(List<AIPDto> in, Exception exception) {
        return buildDefaultStepErrorInfo("FINAL",
                                         exception,
                                         String.format("Persisting SIP and AIP from SIP \"%s\" fails.",
                                                       job.getCurrentEntity().getProviderId()),
                                         IngestErrorType.FINAL);
    }
}
