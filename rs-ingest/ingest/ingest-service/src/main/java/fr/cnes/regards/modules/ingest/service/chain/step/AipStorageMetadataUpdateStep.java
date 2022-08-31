/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.plugin.IAIPStorageMetadataUpdate;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

/**
 * Step that updates request StorageMetadata if the location matches the one expected by the plugin
 *
 * @author Léo Mieulet
 */
public class AipStorageMetadataUpdateStep extends AbstractIngestStep<Set<StorageMetadata>, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AipStorageMetadataUpdateStep.class);

    public AipStorageMetadataUpdateStep(IngestProcessingJob job, IngestProcessingChain ingestChain) {
        super(job, ingestChain);
    }

    @Override
    protected Void doExecute(Set<StorageMetadata> storageMetadata) throws ProcessingStepException {
        job.getCurrentRequest().setStep(IngestRequestStep.LOCAL_AIP_STORAGE_METADATA_UPDATE);
        Optional<PluginConfiguration> conf = ingestChain.getAipStorageMetadataPlugin();
        if (conf.isPresent()) {
            IAIPStorageMetadataUpdate aipStorageMetadataUpdate = this.getStepPlugin(conf.get().getBusinessId());
            try {
                // update current request storage metadata
                job.getCurrentRequest()
                   .getMetadata()
                   .setStorages(aipStorageMetadataUpdate.getStorageMetadata(storageMetadata));
            } catch (ModuleException e) {
                throw new ProcessingStepException(e);
            }
        } else {
            LOGGER.debug("No AIP storage metadata update plugin on chain \"{}\"",
                         job.getCurrentRequest().getMetadata().getIngestChain());
        }
        return null;
    }

    @Override
    protected void doAfterError(Set<StorageMetadata> in, Optional<ProcessingStepException> e) {
        String error = "unknown cause";
        if (e.isPresent()) {
            error = e.get().getMessage();
        }
        handleRequestError(String.format("Updating AIP storage metadata from AIP \"%s\" fails. Cause : %s",
                                         job.getCurrentEntity().getProviderId(),
                                         error));
    }
}
