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
package fr.cnes.regards.modules.ingest.service.chain;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;

/**
 *
 * This job manages processing chain for AIP generation from a SIP
 * @author Marc Sordi
 *
 */
public class IngestProcessingJob extends AbstractJob<Void> {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestProcessingJob.class);

    public static final String CHAIN_NAME_PARAMETER = "chain";

    @Autowired
    private IIngestProcessingChainRepository processingChainRepository;

    private IngestProcessingChain processingChain;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {

        // Retrieve processing chain from parameters
        String processingChainName = getValueFor(parameters, CHAIN_NAME_PARAMETER);

        // Load processing chain
        processingChain = processingChainRepository.findByName(processingChainName);
        if (processingChain == null) {
            String message = String.format("No related chain has been found for value \"%s\"", processingChainName);
            handleInvalidParameter(CHAIN_NAME_PARAMETER, message);
        }
    }

    @Override
    public void run() {

        // Step 1 : optional preprocessing
        // TODO
        // PluginConfiguration conf = processingChain.getPreProcessingPlugin();

        // Step 2 : required validation
        // TODO

        // Step 3 : required AIP generation
        // TODO

        // Step 4 : optional AIP tagging
        // TODO

        // Step 5
        // TODO : store AIPs in storage client! not a plugin in the processing chain!

        // Step 6 : optional postprocessing
        // TODO
    }
}
