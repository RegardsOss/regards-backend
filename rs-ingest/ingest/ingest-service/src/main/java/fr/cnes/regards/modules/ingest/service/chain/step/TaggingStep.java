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

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.plugin.IAipTagging;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import fr.cnes.regards.modules.storage.domain.AIP;

/**
 * Tagging step is used to tag {@link AIP}(s) calling {@link IAipTagging#tag(List)}.
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public class TaggingStep extends AbstractIngestStep<List<AIP>, Void> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TaggingStep.class);

    public TaggingStep(IngestProcessingJob job) {
        super(job);
    }

    @Override
    protected Void doExecute(List<AIP> aips) throws ProcessingStepException {
        Optional<PluginConfiguration> conf = processingChain.getTagPlugin();
        if (conf.isPresent()) {
            IAipTagging tagging = this.getStepPlugin(conf.get().getId());
            aips.forEach(aip -> LOGGER.debug("Tagging AIP \"{}\" from SIP \"{}\"", aip.getId(), aip.getProviderId()));
            tagging.tag(aips);
        } else {
            LOGGER.debug("No AIP tagging for SIP \"{}\"", aips.get(0).getProviderId());
        }
        return null;
    }

    @Override
    protected void doAfterError(List<AIP> pIn) {
        updateSIPEntityState(SIPState.AIP_GEN_ERROR);
    }
}
