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

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.ObjectError;

import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.plugin.ISipValidation;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;

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
        LOGGER.debug("Validating SIP \"{}\"", sip.getId());
        PluginConfiguration conf = ingestChain.getValidationPlugin();
        ISipValidation validation = this.getStepPlugin(conf.getBusinessId());
        Errors errors = new MapBindingResult(new HashMap<>(), sip.getId());
        validation.validate(sip, errors);

        if (errors.hasErrors()) {
            for (ObjectError error : errors.getAllErrors()) {
                LOGGER.error("SIP \"{}\" validation error : {}", sip.getId(), error.toString());
                addError(error.toString());
            }
            throw new ProcessingStepException(String.format("Invalid SIP \"%s\"", sip.getId()));
        }

        // On success
        return null;
    }

    @Override
    protected void doAfterError(SIP sip) {
        handleRequestError(String.format("Validation fails for SIP \"%s\"", sip.getId()));
    }
}
