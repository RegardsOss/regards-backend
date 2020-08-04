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

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.ingest.dao.IAIPPostProcessRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.plugin.ISipPostprocessing;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.AIPPostProcessRequest;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.PostProcessResult;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.service.request.RequestService;

/**
 *
 * @author Sébastien Binda
 * @author Iliana Ghazali
 */
public class IngestPostProcessingJob extends AbstractJob<Void> {

    @Autowired
    private IIngestProcessingChainRepository processingChainRepository;

    @Autowired
    private IAIPPostProcessRequestRepository aipPostProcessRequestRepository;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private RequestService requestService;

    public static final String AIP_POST_PROCESS_REQUEST_IDS = "AIP_POST_PROCESS_REQUEST_IDS";

    private List<AIPPostProcessRequest> requests;


    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        Set<Long> postProcessRequestIds = getValue(parameters, AIP_POST_PROCESS_REQUEST_IDS);
        // Retrieve list of AIP post process requests to handle
        requests = aipPostProcessRequestRepository.findByIdIn(postProcessRequestIds);
    }

    @Override
    public void run() {
        logger.debug("[INGEST POST PROCESSING JOB] Running job for {} AIPPostProcess(s) requests", requests.size());

        // Init params
        long start = System.currentTimeMillis();
        List<AIPEntity> aipEntities = new ArrayList<>();
        boolean interrupted = Thread.currentThread().isInterrupted();//for hibernate transaction

        // Iterate on requests and add aip to list of aips
        Iterator<AIPPostProcessRequest> requestIter = requests.iterator();
        while (requestIter.hasNext() && !interrupted) {
            AIPPostProcessRequest request = requestIter.next();
            aipEntities.add(request.getAip());
            interrupted = Thread.currentThread().isInterrupted();
        }

        // If thread was interrupted, put all requests to ABORTED state
        interrupted = Thread.interrupted();
        if (interrupted) {
            requests.forEach(req -> req.setState(InternalRequestState.ABORTED));
            aipPostProcessRequestRepository.saveAll(requests);
            Thread.currentThread().interrupt();
        } else {
            // Run postprocessing plugin
            PostProcessResult postProcessResult = null;
            try {
                ISipPostprocessing plugin = pluginService.getPlugin(requests.get(0).getConfig().getPostProcessingPluginBusinnessId());
                postProcessResult = plugin.postprocess(aipEntities);

            } catch (ModuleException | NotAvailablePluginConfigurationException e) {
                logger.error("Post processing plugin doest not exists or is not active", e);
            }

            // If postProcess returns errors
            if(postProcessResult!=null) {
                // Update request state to ERROR
                Map<AIPEntity, String> errors = postProcessResult.getErrors();
                for(Map.Entry error: errors.entrySet()){
                    AIPEntity aip = (AIPEntity) error.getKey();
                    AIPPostProcessRequest req = aipPostProcessRequestRepository.findRequestByAipId(aip.getAipId());
                    req.setState(InternalRequestState.ERROR);
                    aipPostProcessRequestRepository.save(req);
                }
            }

            // Delete successful requests
            List<AIPPostProcessRequest> succeedRequestsToDelete = requests.stream()
                    .filter(request -> (request.getState() != InternalRequestState.ABORTED
                            && request.getState() != InternalRequestState.ERROR)).collect(Collectors.toList());
            aipPostProcessRequestRepository.deleteAll(succeedRequestsToDelete);

        }
        logger.debug("[AIP POST PROCESS JOB] Job handled for {} AIPPostProcessRequest(s) requests in {}ms",
                     requests.size(), System.currentTimeMillis() - start);

    }


  /*

    @Autowired
    private IIngestProcessingChainRepository processingChainRepository;

    @Autowired
    private IPluginService pluginService;

    public static final String AIP_POST_PROCESS_REQUEST_IDS = "AIP_POST_PROCESS_REQUEST_IDS";

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
    }*/

}


