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
import fr.cnes.regards.modules.ingest.service.chain.IngestProcessingJob;

/**
 * Common ingest processing step
 *
 * @author Marc Sordi
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
    public O execute(I in) throws ModuleException {
        O out = doExecute(in);
        doAfter();
        return out;
    }

    protected abstract O doExecute(I in) throws ModuleException;

    protected void doAfter() throws ModuleException {
        job.advanceCompletion();
    }
}
