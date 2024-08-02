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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.step.AbstractProcessingStep;
import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.IngestErrorType;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.service.chain.step.info.ErrorModeHandling;
import fr.cnes.regards.modules.ingest.service.chain.step.info.StepErrorInfo;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Common ingest processing step
 *
 * @author Marc Sordi
 */
public abstract class AbstractIngestStep<I, O> extends AbstractProcessingStep<I, O, IngestProcessingJob> {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

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

    @Override
    public O execute(I in) throws ProcessingStepException {
        errors = new HashSet<>();
        return super.execute(in);
    }

    protected <T> T getStepPlugin(String confId) throws ProcessingStepException {
        try {
            return pluginService.getPlugin(confId);
        } catch (ModuleException e) {
            throw new ProcessingStepException(IngestErrorType.UNEXPECTED, e);
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

    protected IngestRequest handleRequestError(IngestErrorType errorType, String error) {
        Assert.hasText(error, "Error message is required");
        prependError(error);
        IngestRequest currentRequest = job.getCurrentRequest();
        currentRequest.setState(InternalRequestState.ERROR);
        currentRequest.setErrors(errorType, errors);
        return ingestRequestService.saveRequest(currentRequest);
    }

    protected void handleRequestErrorWithJobHandling(IngestErrorType errorType, String error) {
        ingestRequestService.handleIngestJobFailed(this.handleRequestError(errorType, error),
                                                   job.getCurrentEntity(),
                                                   error);
    }

    protected ProcessingStepException throwProcessingStepException(IngestErrorType errorType,
                                                                   String errorMessage,
                                                                   Exception e) {
        addError(errorMessage);
        return new ProcessingStepException(errorType, errorMessage, e);
    }

    protected ProcessingStepException throwProcessingStepException(IngestErrorType errorType, String errorMessage) {
        addError(errorMessage);
        return new ProcessingStepException(errorType, errorMessage);
    }

    @Override
    protected void doAfterError(I in, @Nullable Exception exception) {
        StepErrorInfo stepErrorInfo = getStepErrorInfo(in, exception);
        switch (stepErrorInfo.handleModeError()) {
            case HANDLE_REQUEST_WITH_JOB_CRASH ->
                handleRequestErrorWithJobHandling(stepErrorInfo.errorType(), stepErrorInfo.errorMsg());
            // in this case exception is not expected, only save error request. Job will be handled later on.
            case HANDLE_ONLY_REQUEST_ERROR -> handleRequestError(stepErrorInfo.errorType(), stepErrorInfo.errorMsg());
            case NOTHING_TO_DO -> LOGGER.debug("An error occurred during the processing of request with id \"{}\". "
                                               + "Waiting for admin action to handle this case.",
                                               job.getCurrentRequest().getCorrelationId());
        }
    }

    protected abstract StepErrorInfo getStepErrorInfo(I in, @Nullable Exception exception);

    protected StepErrorInfo buildDefaultStepErrorInfo(String stepName,
                                                      Exception exception,
                                                      String stepExceptionMessage,
                                                      IngestErrorType defaultIngestErrorType) {
        StepErrorInfo stepErrorInfo;
        if (exception instanceof ProcessingStepException processingException) {
            stepErrorInfo = new StepErrorInfo(stepName,
                                              ErrorModeHandling.HANDLE_REQUEST_WITH_JOB_CRASH,
                                              String.format("%s. Cause : %s",
                                                            stepExceptionMessage,
                                                            exception.getMessage()),
                                              (IngestErrorType) processingException.getErrorType());
        } else {
            stepErrorInfo = new StepErrorInfo(stepName,
                                              ErrorModeHandling.HANDLE_ONLY_REQUEST_ERROR,
                                              "unknown cause",
                                              defaultIngestErrorType);
        }
        return stepErrorInfo;
    }
}
