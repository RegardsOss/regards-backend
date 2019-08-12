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

import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.modules.ingest.domain.aip.AIP;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;

/**
 * @author Marc SORDI
 *
 */
public class InternalFinalStep extends AbstractIngestStep<List<AIP>, Void> {

    /**
     * @param job
     * @param ingestChain
     */
    public InternalFinalStep(IngestProcessingJob job, IngestProcessingChain ingestChain) {
        super(job, ingestChain);
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.framework.modules.jobs.domain.step.AbstractProcessingStep#doExecute(java.lang.Object)
     */
    @Override
    protected Void doExecute(List<AIP> in) throws ProcessingStepException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.framework.modules.jobs.domain.step.AbstractProcessingStep#doAfterError(java.lang.Object)
     */
    @Override
    protected void doAfterError(List<AIP> in) {
        // TODO Auto-generated method stub

    }

}
