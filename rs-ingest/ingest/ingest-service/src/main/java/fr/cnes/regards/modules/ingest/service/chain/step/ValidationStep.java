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

import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.plugin.ISipValidation;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Validation step is used to validate {@link SIP} calling {@link ISipValidation#validate(SIP, Errors)}.
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public class ValidationStep extends AbstractIngestStep<SIP, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationStep.class);

    public ValidationStep(IngestProcessingJob job, IngestProcessingChain ingestChain) {
        super(job, ingestChain);
    }

    @Override
    protected Void doExecute(SIP sip) throws ProcessingStepException {
        job.getCurrentRequest().setStep(IngestRequestStep.LOCAL_VALIDATION);

        LOGGER.debug("Validating SIP \"{}\"", sip.getId());
        PluginConfiguration conf = ingestChain.getValidationPlugin();
        ISipValidation validation = this.getStepPlugin(conf.getBusinessId());
        Errors validationErrors = new MapBindingResult(new HashMap<>(), sip.getId());
        validation.validate(sip, validationErrors);

        if (validationErrors.hasErrors()) {
            for (String error : ErrorTranslator.getErrors(validationErrors)) {
                LOGGER.error("SIP \"{}\" validation error : {}", sip.getId(), error);
                addError(error);
            }
            throw new ProcessingStepException(String.format("Invalid SIP \"%s\": %s",
                                                            sip.getId(),
                                                            errors.stream().collect(Collectors.joining(", "))));
        }

        // On success
        return null;
    }

    @Override
    protected void doAfterError(SIP sip, Optional<Exception> e) {
        String error = "unknown cause";
        if (e.isPresent()) {
            error = e.get().getMessage();
        }
        handleRequestError(String.format("Validation fails for SIP \"%s\". Cause : %s", sip.getId(), error));
    }
}
