/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.plugin.ISipPreprocessing;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;

/**
 * Preprocessing step is used to do something before starting real processing calling
 * {@link ISipPreprocessing#preprocess(SIP)}.<br/>
 * If {@link SIP} is passed as reference, this step then calls the
 * {@link ISipPreprocessing#read(fr.cnes.regards.modules.ingest.domain.SIPReference)} method to fulfill {@link SIP}
 * properties.
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public class PreprocessingStep extends AbstractIngestStep<SIP, SIP> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PreprocessingStep.class);

    public PreprocessingStep(IngestProcessingJob job) {
        super(job);
    }

    @Override
    public SIP doExecute(SIP sip) throws ProcessingStepException {
        Optional<PluginConfiguration> conf = processingChain.getPreProcessingPlugin();
        if (conf.isPresent()) {
            LOGGER.debug("Preprocessing for SIP \"{}\"", sip.getId());
            ISipPreprocessing preprocessing = this.getStepPlugin(conf.get().getId());
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
    protected void doAfterError(SIP sip) {
        LOGGER.error("Error prepocessing SIP \"{}\"", sip.getId());
        updateSIPEntityState(SIPState.INVALID);
    }
}
