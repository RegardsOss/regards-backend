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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.modules.ingest.dao.IAIPLightRepository;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.plugin.IAipGeneration;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Generation step is used to generate AIP(s) from specified SIP calling {@link IAipGeneration#generate(SIPEntity, String, fr.cnes.regards.framework.urn.EntityType)} (SIP, UniformResourceName, UniformResourceName, String)}.
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public class GenerationStep extends AbstractIngestStep<SIPEntity, List<AIP>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerationStep.class);

    @Autowired
    private Validator validator;

    @Autowired
    private IAIPLightRepository aipLightRepository;

    public GenerationStep(IngestProcessingJob job, IngestProcessingChain ingestChain) {
        super(job, ingestChain);
    }

    @Override
    protected List<AIP> doExecute(SIPEntity sipEntity) throws ProcessingStepException {
        job.getCurrentRequest().setStep(IngestRequestStep.LOCAL_GENERATION);

        LOGGER.debug("Generating AIP(s) from SIP \"{}\"", sipEntity.getSip().getId());
        PluginConfiguration conf = ingestChain.getGenerationPlugin();
        IAipGeneration generation = this.getStepPlugin(conf.getBusinessId());

        // Retrieve SIP URN from internal identifier
        OaisUniformResourceName sipId = job.getCurrentEntity().getSipIdUrn();
        // Launch AIP generation
        List<AIP> aips = generation.generate(sipEntity, sipId.getTenant(), sipId.getEntityType());
        // Add version to AIP
        for (AIP aip : aips) {
            aip.setVersion(aip.getId().getVersion());
            aip.withEvent(EventType.SUBMISSION.toString(),
                          String.format("AIP created from SIP %s(version %s).",
                                        sipEntity.getProviderId(),
                                        sipId.getVersion()));
        }

        // Validate
        validateAips(aips);
        // Return valid AIPs
        return aips;
    }

    private void validateAips(List<AIP> aips) throws ProcessingStepException {
        // Validate all elements of the flow item
        Errors validationErrors;
        Multimap<String, Integer> versionsByProviderId = HashMultimap.create();
        for (AIP aip : aips) {
            // first handle issues with this aip
            validationErrors = new MapBindingResult(new HashMap<>(), AIP.class.getName());
            validator.validate(aip, validationErrors);
            // now lets handle issues with all aips generated
            String providerId = aip.getProviderId();
            aipLightRepository.findAllByProviderId(providerId)
                              .forEach(aipLight -> versionsByProviderId.put(providerId, aipLight.getVersion()));
            if (!versionsByProviderId.put(providerId, aip.getVersion())) {
                String error = String.format("Version %s already exists for the providerId %s.",
                                             aip.getVersion(),
                                             providerId);
                validationErrors.rejectValue("version", error);
            }
            if (validationErrors.hasErrors()) {
                ErrorTranslator.getErrors(validationErrors).forEach(e -> {
                    String error = String.format("AIP %s has validation issues: %s", aip.getId().toString(), e);
                    addError(error);
                    LOGGER.error(error);
                });
            }
        }
        if (!errors.isEmpty()) {
            throw new ProcessingStepException(String.format("Validation errors for AIPs generated from SIP %s: %s",
                                                            job.getCurrentEntity().getProviderId(),
                                                            errors.stream().collect(Collectors.joining(", "))));
        }
    }

    @Override
    protected void doAfterError(SIPEntity sip, Optional<ProcessingStepException> e) {
        String error = "unknown cause";
        if (e.isPresent()) {
            error = e.get().getMessage();
        }
        handleRequestError(String.format("Generation fails for AIP(s) of SIP \"%s\". Cause : %s",
                                         sip.getSip().getId(),
                                         error));
    }
}
