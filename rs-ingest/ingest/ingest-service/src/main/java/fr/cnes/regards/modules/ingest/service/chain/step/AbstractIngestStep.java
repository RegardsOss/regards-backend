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

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.step.AbstractProcessingStep;
import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;

/**
 * Common ingest processing step
 *
 * @author Marc Sordi
 */
public abstract class AbstractIngestStep<I, O> extends AbstractProcessingStep<I, O, IngestProcessingJob> {

    protected final IngestProcessingChain ingestChain;

    @Autowired
    protected IPluginService pluginService;

    @Autowired
    protected IIngestRequestService ingestRequestService;

    protected Set<String> errors;

    public AbstractIngestStep(IngestProcessingJob job, IngestProcessingChain ingestChain) {
        super(job);
        this.ingestChain = ingestChain;
    }

    protected <T> T getStepPlugin(String confId) throws ProcessingStepException {
        try {
            return pluginService.getPlugin(confId);
        } catch (ModuleException | NotAvailablePluginConfigurationException e) {
            throw new ProcessingStepException(e);
        }
    }

    protected void addError(String error) {
        if (errors == null) {
            errors = new HashSet<>();
        }
        errors.add(error);
    }

    protected void handleRequestError(String error) {
        Assert.hasText(error, "Error message is required");
        addError(error);
        job.getCurrentRequest().setErrors(errors);
        ingestRequestService.handleIngestJobFailed(job.getCurrentRequest(), job.getCurrentEntity());
    }
}
