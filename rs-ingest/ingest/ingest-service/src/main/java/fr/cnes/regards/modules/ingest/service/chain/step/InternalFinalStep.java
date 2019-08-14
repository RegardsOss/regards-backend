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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;

/**
 *
 * Persist all generated entities in database : {@link SIPEntity} including {@link SIP} and {@link AIPEntity} including {@link AIP}(s)
 *
 * @author Marc SORDI
 *
 */
public class InternalFinalStep extends AbstractIngestStep<List<AIP>, Void> {

    @Autowired
    private ISIPService sipService;

    @Autowired
    private IAIPService aipService;

    public InternalFinalStep(IngestProcessingJob job, IngestProcessingChain ingestChain) {
        super(job, ingestChain);
    }

    @Override
    protected Void doExecute(List<AIP> aips) throws ProcessingStepException {

        // Get on work SIP entity
        SIPEntity sipEntity = job.getCurrentEntity();
        sipService.saveSIPEntity(sipEntity);

        // Build AIP entities and save them
        aipService.createAndSave(sipEntity, aips);

        return null;
    }

    @Override
    protected void doAfterError(List<AIP> in) {
        handleRequestError(String.format("Persisting SIP and AIP from SIP \"%s\" fails",
                                         job.getCurrentEntity().getProviderId()));
    }

}
