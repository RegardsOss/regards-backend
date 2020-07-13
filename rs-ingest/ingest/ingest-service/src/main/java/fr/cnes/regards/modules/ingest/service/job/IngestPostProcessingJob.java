/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.job;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.plugin.ISipPostprocessing;

/**
 *
 * @author Sébastien Binda
 *
 */
public class IngestPostProcessingJob extends AbstractJob<Void> {

    @Autowired
    private IIngestProcessingChainRepository processingChainRepository;

    @Autowired
    private IPluginService pluginService;

    public static final String INGEST_CHAIN_ID_PARAMETER = "chain_id";

    public static final String AIPS_PARAMETER = "aips";

    private Optional<IngestProcessingChain> ingestChain = Optional.empty();

    private final Set<AIPEntity> aipEntities = Sets.newHashSet();

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        ingestChain = processingChainRepository.findById(parameters.get(INGEST_CHAIN_ID_PARAMETER).getValue());
        aipEntities.addAll(parameters.get(AIPS_PARAMETER).getValue());
    }

    @Override
    public void run() {
        if (ingestChain.isPresent() && ingestChain.get().getPostProcessingPlugin().isPresent()) {
            try {
                ISipPostprocessing plugin = pluginService
                        .getPlugin(ingestChain.get().getPostProcessingPlugin().get().getBusinessId());
                plugin.postprocess(ingestChain.get(), aipEntities, this);
            } catch (ModuleException | NotAvailablePluginConfigurationException e) {
                logger.error("Post processing plugin doest not exists or is not active", e);
            }
        } else {
            logger.warn("Ingest processing chain doest not exists anymore or no post processing plugin to apply");
        }
    }

    @Override
    public int getCompletionCount() {
        return this.aipEntities.size();
    }

}
