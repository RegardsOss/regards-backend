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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.plugin.IGenerateAIP;
import fr.cnes.regards.modules.ingest.service.chain.IngestProcessingJob;
import fr.cnes.regards.modules.storage.domain.AIP;

/**
 * Generation step is used to generate AIP(s) from specified SIP calling {@link IGenerateAIP#generate(SIP)}.
 *
 * @author Marc Sordi
 *
 */
public class GenerationStep extends AbstractProcessingStep<SIP, List<AIP>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerationStep.class);

    public GenerationStep(IngestProcessingJob job) {
        super(job);
    }

    @Override
    protected List<AIP> doExecute(SIP sip) throws ModuleException {
        LOGGER.debug("Generating AIP(s) from SIP \"{}\"", sip.getId());
        PluginConfiguration conf = processingChain.getGenerationPlugin();
        IGenerateAIP generation = pluginService.getPlugin(conf.getId());
        return generation.generate(sip);
    }
}
