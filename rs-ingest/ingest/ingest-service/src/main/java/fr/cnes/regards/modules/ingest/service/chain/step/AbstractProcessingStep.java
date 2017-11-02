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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.exception.ProcessingStepException;
import fr.cnes.regards.modules.ingest.service.chain.IngestProcessingJob;

/**
 * Common ingest processing step
 *
 * @author Marc Sordi
 * @auhtor Sébastien Binda
 */
public abstract class AbstractProcessingStep<I, O> implements IProcessingStep<I, O> {

    protected final IngestProcessingChain processingChain;

    protected final IPluginService pluginService;

    protected final IngestProcessingJob job;

    public AbstractProcessingStep(IngestProcessingJob job) {
        this.job = job;
        this.processingChain = job.getProcessingChain();
        this.pluginService = job.getPluginService();
    }

    @Override
    public O execute(I in) throws ProcessingStepException {
        boolean error = true;
        try {
            O out = doExecute(in);
            error = false;
            return out;
        } finally {
            if (error) {
                doAfterStepError(in);
            } else {
                doAfter(in);
            }
        }
    }

    protected abstract O doExecute(I in) throws ProcessingStepException;

    protected abstract void doAfterStepError(I in);

    protected void doAfter(I in) throws ProcessingStepException {
        job.advanceCompletion();
        this.doAfterStepSuccess(in);
    }

    protected abstract void doAfterStepSuccess(I in);

    protected SIPEntity updateSIPEntityState(SIPState newEntitySIPState) {
        SIPEntity entity = this.job.getIngestProcessingService().updateSIPEntityState(this.job.getEntity().getId(),
                                                                                      newEntitySIPState);
        return entity;
    }

    protected <T> T getStepPlugin(Long confId) throws ProcessingStepException {
        try {
            return pluginService.getPlugin(confId);
        } catch (ModuleException e) {
            throw new ProcessingStepException(e);
        }
    }
}
