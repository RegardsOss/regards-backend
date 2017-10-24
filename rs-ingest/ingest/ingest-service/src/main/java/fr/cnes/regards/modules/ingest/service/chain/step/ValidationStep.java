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

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.plugin.IValidateSIP;
import fr.cnes.regards.modules.ingest.service.chain.IngestProcessingJob;

/**
 * Validation step is used to validate {@link SIP} calling {@link IValidateSIP#validate(SIP, Errors)}.
 *
 * @author Marc Sordi
 *
 */
public class ValidationStep extends AbstractProcessingStep<SIP, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationStep.class);

    public ValidationStep(IngestProcessingJob job) {
        super(job);
    }

    @Override
    protected Void doExecute(SIP sip) throws ModuleException {
        LOGGER.debug("Validating SIP \"{}\"", sip.getId());
        PluginConfiguration conf = processingChain.getValidationPlugin();
        IValidateSIP validation = pluginService.getPlugin(conf.getId());
        Errors errors = new MapBindingResult(new HashMap<>(), sip.getId());
        validation.validate(sip, errors);

        if (errors.hasErrors()) {
            errors.getAllErrors().forEach(error -> LOGGER.error("SIP \"{}\" validation error : {}", sip.getId(),
                                                                error.getDefaultMessage()));
            throw new JobRuntimeException(String.format("Invalid SIP \"%s\"", sip.getId()));
        }
        return null;
    }
}
