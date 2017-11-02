/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.ingest.domain.entity.AIPState;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.exception.ProcessingStepException;
import fr.cnes.regards.modules.ingest.service.chain.IngestProcessingJob;
import fr.cnes.regards.modules.storage.domain.AIP;

/**
 * Store generated AIP into database to be handled by scheduled process and sent to storage microservice
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public class StoreStep extends AbstractProcessingStep<List<AIP>, Void> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreStep.class);

    public StoreStep(IngestProcessingJob job) {
        super(job);
    }

    @Override
    protected Void doExecute(List<AIP> in) throws ProcessingStepException {
        // Store generated aips in db with raw object.
        // Created AIPEntities will be handled by a scheduled task to be sent to archival storage microservice
        in.forEach(aip -> this.job.getIngestProcessingService().createAIP(this.job.getEntity().getId(),
                                                                          AIPState.CREATED, aip));
        return null;
    }

    @Override
    protected void doAfterStepError(List<AIP> pIn) {
        this.updateSIPEntityState(SIPState.AIP_GEN_ERROR);
    }

    @Override
    protected void doAfterStepSuccess(List<AIP> pIn) {
        this.updateSIPEntityState(SIPState.AIP_CREATED);
    }

}
