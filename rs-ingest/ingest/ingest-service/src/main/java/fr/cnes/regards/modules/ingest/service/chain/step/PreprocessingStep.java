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

import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.oais.dto.sip.SIPReference;
import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.plugin.ISipPreprocessing;
import fr.cnes.regards.modules.ingest.domain.request.IngestErrorType;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.service.chain.step.info.StepErrorInfo;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;

import java.util.Optional;

/**
 * Preprocessing step is used to do something before starting real processing calling
 * {@link ISipPreprocessing#preprocess(SIPDto)}.<br/>
 * If {@link SIPDto} is passed as reference, this step then calls the
 * {@link ISipPreprocessing#read(SIPReference} method to fulfill {@link SIPDto}
 * properties.
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public class PreprocessingStep extends AbstractIngestStep<SIPDto, SIPDto> {

    public PreprocessingStep(IngestProcessingJob job, IngestProcessingChain ingestChain) {
        super(job, ingestChain);
    }

    @Override
    public SIPDto doExecute(SIPDto sip) throws ProcessingStepException {
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
    protected StepErrorInfo getStepErrorInfo(SIPDto sip, Exception exception) {
        return buildDefaultStepErrorInfo("PREPROCESSING",
                                         exception,
                                         String.format("Preprocessing fails for SIP \"%s\".", sip.getId()),
                                         IngestErrorType.PREPROCESSING);
    }
}
