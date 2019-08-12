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
package fr.cnes.regards.modules.ingest.service.chain.step;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.modules.ingest.domain.aip.AIP;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;

/**
 * Store generated AIP into database to be handled by scheduled process and sent to storage microservice
 * @author Marc Sordi
 * @author Sébastien Binda
 */
@Deprecated
public class StoreStep extends AbstractIngestStep<List<AIP>, Void> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(StoreStep.class);

    public StoreStep(IngestProcessingJob job, IngestProcessingChain ingestChain) {
        super(job, ingestChain);
    }

    @Override
    protected Void doExecute(List<AIP> aips) throws ProcessingStepException {
        //        try {
        //            this.job.getIngestProcessingService().saveAndSubmitAIP(this.job.getCurrentEntity(), aips);
        //        } catch (EntityNotFoundException e) {
        //            throw new ProcessingStepException(e);
        //        }
        return null;
    }

    @Override
    protected void doAfterError(List<AIP> pIn) {
        //        handleRequestError(String.format("Error storing AIP(s) for SIP \"{}\"", job.);
        //        LOGGER.error(error);
        //        updateCurrentRequestOnError(IngestRequestState.ERROR, error);
    }
}
