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

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.plugin.ISipPostprocessing;
import fr.cnes.regards.modules.ingest.domain.request.IngestRequestStep;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;

/**
 * Postprocessing step is used to do something after {@link AIP}(s) generation calling
 * {@link ISipPostprocessing#postprocess(SIP)}
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public class PostprocessingStep extends AbstractIngestStep<SIP, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostprocessingStep.class);

    public PostprocessingStep(IngestProcessingJob job, IngestProcessingChain ingestChain) {
        super(job, ingestChain, IngestRequestStep.LOCAL_POST_PROCESSING);
    }

    @Override
    protected Void doExecute(SIP sip) throws ProcessingStepException {
        Optional<PluginConfiguration> conf = ingestChain.getPostProcessingPlugin();
        if (conf.isPresent()) {
            LOGGER.debug("Postprocessing for SIP \"{}\"", sip.getId());
            ISipPostprocessing postprocessing = this.getStepPlugin(conf.get().getBusinessId());
            postprocessing.postprocess(sip);
        } else {
            LOGGER.debug("No postprocessing for SIP \"{}\"", sip.getId());
        }
        return null;
    }

    @Override
    protected void doAfterError(SIP sip) {
        handleRequestError(String.format("Post processing fails for SIP \"{}\"", sip.getId()));
    }
}
