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

import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.plugin.IAipTagging;
import fr.cnes.regards.modules.ingest.domain.request.IngestErrorType;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.service.chain.step.info.StepErrorInfo;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;

import java.util.List;
import java.util.Optional;

/**
 * Tagging step is used to tag {@link AIPDto}(s) calling {@link IAipTagging#tag(List)}.
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public class TaggingStep extends AbstractIngestStep<List<AIPDto>, Void> {

    public TaggingStep(IngestProcessingJob job, IngestProcessingChain ingestChain) {
        super(job, ingestChain);
    }

    @Override
    protected Void doExecute(List<AIPDto> aips) throws ProcessingStepException {
        job.getCurrentRequest().setStep(IngestRequestStep.LOCAL_TAGGING);

        Optional<PluginConfiguration> conf = ingestChain.getTagPlugin();
        if (conf.isPresent()) {
            IAipTagging tagging = this.getStepPlugin(conf.get().getBusinessId());
            aips.forEach(aip -> LOGGER.debug("Tagging AIP \"{}\" from SIP \"{}\"", aip.getId(), aip.getProviderId()));
            tagging.tag(aips);
        } else {
            LOGGER.debug("No AIP tagging for SIP \"{}\"", aips.get(0).getProviderId());
        }
        return null;
    }

    @Override
    protected StepErrorInfo getStepErrorInfo(List<AIPDto> aips, Exception exception) {
        return buildDefaultStepErrorInfo("TAGGING",
                                         exception,
                                         String.format("Tagging fails for AIP of SIP \"%s\".",
                                                       job.getCurrentEntity().getProviderId()),
                                         IngestErrorType.TAGGING);
    }
}
