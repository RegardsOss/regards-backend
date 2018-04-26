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
package fr.cnes.regards.modules.ingest.service.job;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SipAIPState;
import fr.cnes.regards.modules.ingest.service.store.IAIPService;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.RejectedAip;

/**
 * This job manages AIP submission (i.e. STORAGE bulk request) for a specified chain.
 *
 * @author Marc Sordi
 *
 */
public class AIPSubmissionJob extends AbstractJob<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPSubmissionJob.class);

    public static final String INGEST_CHAIN_PARAMETER = "chain";

    private String ingestProcessingChain;

    @Autowired
    private IAipClient aipClient;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private Gson gson;

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        ingestProcessingChain = getValue(parameters, INGEST_CHAIN_PARAMETER);
    }

    @Override
    public void run() {

        // Get all AIP ready to store for the current the ingest chain
        Set<AIPEntity> aips = aipService.findAIPToSubmit(ingestProcessingChain);

        // Create AIP collection
        AIPCollection collection = new AIPCollection();
        aips.forEach(aip -> collection.add(aip.getAip()));

        // Submit AIP collection to STORAGE
        try {
            FeignSecurityManager.asSystem();
            ResponseEntity<List<RejectedAip>> response = aipClient.store(collection);
            handleResponse(response.getStatusCode(), response.getBody(), aips);
        } catch (HttpClientErrorException e) {
            // Handle non 2xx or 404 status code
            List<RejectedAip> rejectedAips = null;
            if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                @SuppressWarnings("serial")
                TypeToken<List<RejectedAip>> bodyTypeToken = new TypeToken<List<RejectedAip>>() {
                };
                rejectedAips = gson.fromJson(e.getResponseBodyAsString(), bodyTypeToken.getType());
            }
            handleResponse(e.getStatusCode(), rejectedAips, aips);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    /**
     * Handle STORAGE response
     * @param status
     * @param aips list of submitted AIPs
     */
    private void handleResponse(HttpStatus status, List<RejectedAip> rejectedAips, Set<AIPEntity> aips) {
        switch (status) {
            case CREATED:
                // All AIP are valid
                for (AIPEntity aip : aips) {
                    aip.setState(AIPState.VALID);
                    aipService.save(aip);
                }
                break;
            case PARTIAL_CONTENT:
                // Some AIP are rejected
                Map<String, List<String>> rejectionCausesByIpId = new HashMap<>();
                if (rejectedAips != null) {
                    rejectedAips.forEach(aip -> rejectionCausesByIpId.put(aip.getIpId(), aip.getRejectionCauses()));
                }
                for (AIPEntity aip : aips) {
                    if (rejectionCausesByIpId.containsKey(aip.getIpId())) {
                        rejectAip(aip.getIpId(), rejectionCausesByIpId.get(aip.getIpId()));
                    } else {
                        aip.setState(AIPState.VALID);
                        aipService.save(aip);
                    }
                }
                break;
            case UNPROCESSABLE_ENTITY:
                // All AIP rejected
                if (rejectedAips != null) {
                    for (RejectedAip aip : rejectedAips) {
                        rejectAip(aip.getIpId(), aip.getRejectionCauses());
                    }
                }
                break;
            default:
                String message = String.format("AIP submission failure for ingest chain \"%s\"", ingestProcessingChain);
                LOGGER.error(message);
                throw new JobRuntimeException(message);
        }
    }

    private void rejectAip(String aipId, List<String> rejectionCauses) {
        LOGGER.warn("AIP {} has been rejected by archival storage microservice for store action", aipId);
        StringJoiner errorMessage = new StringJoiner(", ");
        rejectionCauses.forEach(cause -> errorMessage.add(cause));
        aipService.setAipInError(aipId, SipAIPState.REJECTED, errorMessage.toString());
    }
}
