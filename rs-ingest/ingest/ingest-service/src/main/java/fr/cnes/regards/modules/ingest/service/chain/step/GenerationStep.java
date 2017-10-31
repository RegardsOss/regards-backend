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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.exception.ProcessingStepException;
import fr.cnes.regards.modules.ingest.domain.plugin.IAipGeneration;
import fr.cnes.regards.modules.ingest.service.chain.IngestProcessingJob;
import fr.cnes.regards.modules.storage.domain.AIP;

/**
 * Generation step is used to generate AIP(s) from specified SIP calling {@link IAipGeneration#generate(SIP)}.
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
public class GenerationStep extends AbstractProcessingStep<SIP, List<AIP>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerationStep.class);

    public GenerationStep(IngestProcessingJob job) {
        super(job);
    }

    @Override
    protected List<AIP> doExecute(SIP sip) throws ProcessingStepException {
        LOGGER.debug("Generating AIP(s) from SIP \"{}\"", sip.getId());
        PluginConfiguration conf = processingChain.getGenerationPlugin();
        IAipGeneration generation = this.getStepPlugin(conf.getId());

        // Retrieve SIP URN from internal identifier
        UniformResourceName sipUrn = UniformResourceName.fromString(job.getEntity().getIpId());
        // Compute AIP URN from SIP one
        UniformResourceName ipId = new UniformResourceName(OAISIdentifier.AIP, sipUrn.getEntityType(),
                sipUrn.getTenant(), sipUrn.getEntityId(), sipUrn.getVersion());
        // Launch AIP generation
        return generation.generate(sip, ipId, sipUrn.toString());
    }

    @Override
    protected void doAfterStepError(SIP sip) {
        LOGGER.error("Error generating AIP(s) for SIP \"{}\"", sip.getId());
        this.updateSIPEntityState(SIPState.AIP_GEN_ERROR);
    }

    @Override
    protected void doAfterStepSuccess(SIP pIn) {
        this.updateSIPEntityState(SIPState.AIP_GENERATED);
    }
}
