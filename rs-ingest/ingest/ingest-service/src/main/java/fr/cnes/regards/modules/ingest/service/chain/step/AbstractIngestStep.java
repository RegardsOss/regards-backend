/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.modules.jobs.domain.step.AbstractProcessingStep;
import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;

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

    @Override
    public O execute(I in) throws ProcessingStepException {
        errors = new HashSet<>();
        return super.execute(in);
    }

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

    /**
     * Prepend the error message list with the one provided
     */
    protected void prependError(String error) {
        Set<String> updatedErrors = new HashSet<>();
        updatedErrors.add(error);
        if (errors != null) {
            updatedErrors.addAll(errors);
        }
        errors = updatedErrors;
    }

    protected void handleRequestError(String error) {
        Assert.hasText(error, "Error message is required");
        prependError(error);
        job.getCurrentRequest().setState(InternalRequestState.ERROR);
        job.getCurrentRequest().setErrors(errors);
        ingestRequestService.handleIngestJobFailed(job.getCurrentRequest(), job.getCurrentEntity(), error);
    }

    protected ProcessingStepException throwProcessingStepException(String error, Exception e) {
        addError(error);
        return new ProcessingStepException(error, e);
    }

    protected ProcessingStepException throwProcessingStepException(String error) {
        addError(error);
        return new ProcessingStepException(error);
    }
}
