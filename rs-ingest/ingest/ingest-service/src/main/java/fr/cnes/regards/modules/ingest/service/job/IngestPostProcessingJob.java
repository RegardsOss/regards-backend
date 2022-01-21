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
package fr.cnes.regards.modules.ingest.service.job;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.reflect.TypeToken;

import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.ingest.dao.IAIPPostProcessRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.plugin.ISipPostprocessing;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.AIPPostProcessRequest;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.PostProcessResult;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;

/**
 * This job is used to create XML files from aip contents.
 * @author Iliana Ghazali
 * @author Sébastien Binda
 */

public class IngestPostProcessingJob extends AbstractJob<Void> {

    @Autowired
    private IAIPPostProcessRequestRepository aipPostProcessRequestRepository;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private SessionNotifier sessionNotifier;

    public static final String AIP_POST_PROCESS_REQUEST_IDS = "AIP_POST_PROCESS_REQUEST_IDS";

    private Map<Long, AIPPostProcessRequest> requests;

    private final Map<String, Long> mapAipReq = new HashMap<>();

    private final Map<String, List<String>> mapPluginAip = new HashMap<>();

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        Type type = new TypeToken<List<Long>>() {

        }.getType();
        List<Long> postProcessRequestIds = getValue(parameters, AIP_POST_PROCESS_REQUEST_IDS, type);
        // Convert Request List to Map
        this.requests = listRequestsToMap(aipPostProcessRequestRepository.findAllById(postProcessRequestIds));

    }

    @Override
    public void run() {
        logger.debug("[INGEST POST PROCESSING JOB] Running job for {} AIPPostProcess(s) requests", requests.size());
        // Init params
        long start = System.currentTimeMillis();

        // Create links between aipId/reqId and pluginBusinessId/set(aipIds)
        createMapsAipReqPluginId();

        //Run plugins for all requests
        launchPlugins();

        logger.debug("[AIP POSTPROCESS JOB] Job handled for {} AIPPostProcesses(s) requests in {}ms",
                     this.requests.size(), System.currentTimeMillis() - start);

    }

    /**
     * PostProcess aips with their associated plugins
     */
    private void launchPlugins() {
        // Init params
        PostProcessResult postProcessResult;
        ISipPostprocessing plugin;
        Set<String> aipIdsSuccess = new HashSet<>();
        String pluginBusinessId;
        List<String> aipIds;

        boolean isInterrupted = false;

        // Loop on map businessId-aipIds
        for (Map.Entry<String, List<String>> pluginToLaunch : this.mapPluginAip.entrySet()) {
            // Launch plugin by businessId
            pluginBusinessId = pluginToLaunch.getKey();
            aipIds = pluginToLaunch.getValue();
            logger.debug("Launch plugin {}", pluginBusinessId);
            try {
                plugin = pluginService.getPlugin(pluginBusinessId);
                postProcessResult = plugin.postprocess(getAipById(aipIds));

                // Check if process was interrupted
                if (postProcessResult.isInterrupted() || Thread.currentThread().isInterrupted()) {
                    isInterrupted = true;
                    break;
                }

                // Update Local Requests
                // If postProcess returns errors - put all requests corresponding to failed aips to ERROR state
                if (!postProcessResult.getErrors().isEmpty()) {
                    putReqError(postProcessResult.getErrors());

                }
                // If Success - add successful aipIds to list
                if (!postProcessResult.getSuccesses().isEmpty()) {
                    aipIdsSuccess.addAll(postProcessResult.getSuccesses());
                }
            } catch (Exception e) {
                //put all requests in error
                logger.error("Exception : ", e);
                Map<String, Set<String>> errors = new HashMap<>();
                for (String aipId : aipIds) {
                    errors.put(aipId, Collections.singleton(e.getMessage()));
                }
                putReqError(errors);
            }
            advanceCompletion();
        }

        // Update BDD (update error requests)
        // If interrupted all requests not handled are already set in ABORTED status at job start
        aipPostProcessRequestRepository.saveAll(this.requests.values());

        // Delete requests processed successfully
        deleteSuccessReq(aipIdsSuccess);

        if (isInterrupted) {
            // Restart thread
            Thread.currentThread().interrupt();
        }

    }

    //--------------------------------------
    // --------- MAPPING CREATION ----------
    //--------------------------------------

    /**
     * Create two mappings.
     * One between aipId and its corresponding reqId.
     * The other between the plugin id and the set of aipIds it has to process.
     */
    private void createMapsAipReqPluginId() {
        this.requests.forEach((reqId, req) -> {
            String aipId = req.getAip().getAipId();
            String pluginId = req.getConfig().getPostProcessingPluginBusinnessId();

            // Build aipId and reqId mapping
            this.mapAipReq.put(aipId, reqId);

            // Build plugin businessId and AIP ID mapping
            // create businessId key if not existing
            if (!this.mapPluginAip.containsKey(pluginId)) {
                this.mapPluginAip.put(pluginId, new ArrayList<>());
            }
            // add aips to related businessId
            this.mapPluginAip.get(pluginId).add(aipId);
        });
    }

    //--------------------------------------
    // ----- TO SEARCH REQ/AIP BY IDS ------
    //--------------------------------------

    /**
     * Transform list of requests to map of reqIds - requests and put all request states to ABORTED state
     * @param list list of requests
     * @return map of reqIds - aborted requests
     */
    private Map<Long, AIPPostProcessRequest> listRequestsToMap(List<AIPPostProcessRequest> list) {
        return list.stream().collect(Collectors.toMap(AIPPostProcessRequest::getId, request -> {
            // Set all state requests to aborded at init so that all requests not postprocessed keep ABORTED state
            request.setState(InternalRequestState.ABORTED);
            return request;
        }));
    }

    /**
     * Get set of aips to postprocess by provided their ids.
     * @param aipIdSet set of aipsId
     * @return set of aips
     */
    private Set<AIPEntity> getAipById(List<String> aipIdSet) {
        Set<AIPEntity> aipSet = new LinkedHashSet<>();
        Long reqId;
        for (String aipId : aipIdSet) {
            //Get req.aip and add to set
            reqId = mapAipReq.get(aipId);
            aipSet.add(this.requests.get(reqId).getAip());
        }
        return aipSet;
    }

    //--------------------------------------
    // ---------- REQUEST UPDATES ----------
    //--------------------------------------

    /**
     * Update status of requests in errors to ERROR
     * @param errorMap map of aipIds in error and the corresponding list of error messages
     */
    private void putReqError(Map<String, Set<String>> errorMap) {
        Long reqId;
        String aipId;
        Set<String> errorMsg;
        // map of aipIds - linked errors
        for (Map.Entry<String, Set<String>> error : errorMap.entrySet()) {
            aipId = error.getKey();
            reqId = mapAipReq.get(aipId);
            errorMsg = error.getValue();
            AIPPostProcessRequest request = this.requests.get(reqId);
            request.setState(InternalRequestState.ERROR);
            request.setErrors(errorMsg);
            sessionNotifier.incrementPostProcessError(request);
            logger.error("Request {} corresponding to AIP {} in error. Caused by [{}]", reqId, aipId,
                         String.join(",\n", errorMsg));
        }
    }

    /**
     * Delete requests successfully processed
     */
    private void deleteSuccessReq(Set<String> aipIdsSuccess) {
        // Get all reqIds corresponding to aipIds returned as successes
        Set<Long> reqIdsSuccess = new HashSet<>();
        for (Map.Entry<String, Long> e : this.mapAipReq.entrySet()) {
            if (aipIdsSuccess.contains(e.getKey())) {
                Long value = e.getValue();
                reqIdsSuccess.add(value);
            }
        }
        // Get all successful requests to delete
        List<AIPPostProcessRequest> succeedRequestsToDelete = new ArrayList<>();
        for (Map.Entry<Long, AIPPostProcessRequest> req : this.requests.entrySet()) {
            if (reqIdsSuccess.contains(req.getKey())) {
                succeedRequestsToDelete.add(req.getValue());
                sessionNotifier.incrementPostProcessSuccess(req.getValue());
            }
        }
        //Delete successful requests
        aipPostProcessRequestRepository.deleteAll(succeedRequestsToDelete);
        logger.debug("AIPs in success deleted from database");
    }

    @Override
    public int getCompletionCount() {
        return mapPluginAip.keySet().size();
    }
}