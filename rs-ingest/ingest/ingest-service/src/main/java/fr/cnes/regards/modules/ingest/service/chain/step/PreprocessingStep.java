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

import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.plugin.ISipPreprocessing;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Preprocessing step is used to do something before starting real processing calling
 * {@link ISipPreprocessing#preprocess(SIP)}.<br/>
 * If {@link SIP} is passed as reference, this step then calls the
 * {@link ISipPreprocessing#read(fr.cnes.regards.modules.ingest.dto.sip.SIPReference)} method to fulfill {@link SIP}
 * properties.
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public class PreprocessingStep extends AbstractIngestStep<SIP, SIP> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreprocessingStep.class);

    public PreprocessingStep(IngestProcessingJob job, IngestProcessingChain ingestChain) {
        super(job, ingestChain);
    }

    @Override
    public SIP doExecute(SIP sip) throws ProcessingStepException {
        job.getCurrentRequest().setStep(IngestRequestStep.LOCAL_PRE_PROCESSING);

        Optional<PluginConfiguration> conf = ingestChain.getPreProcessingPlugin();
        if (conf.isPresent()) {
            LOGGER.debug("Preprocessing for SIP \"{}\"", sip.getId());
            ISipPreprocessing preprocessing = this.getStepPlugin(conf.get().getBusinessId());
            preprocessing.preprocess(sip);
            if (sip.isRef()) {
                LOGGER.debug("Reading referenced SIP \"{}\"", sip.getId());
                // Override SIP value reading referenced file
                return preprocessing.read(sip.getRef());
            }
        } else {
            LOGGER.debug("No preprocessing for SIP \"{}\"", sip.getId());
        }
        return sip;
    }

    @Override
    protected void doAfterError(SIP sip, Optional<Exception> e) {
        String error = "unknown cause";
        if (e.isPresent()) {
            error = e.get().getMessage();
        }
        handleRequestError(String.format("Preprocessing fails for SIP \"%s\". Cause : %s", sip.getId(), error));
    }
}
