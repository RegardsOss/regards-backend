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

import java.util.ArrayList;
import java.util.List;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.step.AbstractProcessingStep;
import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;

/**
 * Common ingest processing step
 * @author Marc Sordi
 */
public abstract class AbstractIngestStep<I, O> extends AbstractProcessingStep<I, O, IngestProcessingJob> {

    protected final IngestProcessingChain processingChain;

    protected final IPluginService pluginService;

    protected List<String> processingErrors;

    public AbstractIngestStep(IngestProcessingJob job) {
        super(job);
        this.processingChain = job.getProcessingChain();
        this.pluginService = job.getPluginService();
    }

    protected SIPEntity updateSIPEntityState(SIPState newEntitySIPState) {
        job.getCurrentEntity().setState(newEntitySIPState);
        job.getCurrentEntity().setProcessingErrors(processingErrors);
        return job.getIngestProcessingService().updateSIPEntity(job.getCurrentEntity());
    }

    protected <T> T getStepPlugin(Long confId) throws ProcessingStepException {
        try {
            return pluginService.getPlugin(confId);
        } catch (ModuleException | NotAvailablePluginConfigurationException e) {
            throw new ProcessingStepException(e);
        }
    }

    public void addProcessingError(String error) {
        if (processingErrors == null) {
            processingErrors = new ArrayList<>();
        }
        processingErrors.add(error);
    }
}
