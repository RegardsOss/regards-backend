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
package fr.cnes.regards.modules.ingest.service.store;

import java.time.OffsetDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.AIPState;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.service.ISIPService;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.client.IAipEntityClient;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.RejectedAip;

/**
 * Service to handle aip related issues in ingest, including sending bulk request of AIP to store to archival storage microservice.
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 */
@Service
@MultitenantTransactional
public class AIPService implements IAIPService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPService.class);

    @Autowired
    private ISIPService sipService;

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IAipEntityClient aipEntityClient;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private IAipClient aipClient;

    @Autowired
    private Gson gson;

    @Value("${regards.ingest.aips.bulk.request.limit:10000}")
    private Integer bulkRequestLimit;

    @Override
    public void postAIPStorageBulkRequest() {

        // 1. Retrieve all aip ready to be stored
        Set<Long> aipIds = aipRepository.findIdByStateAndLock(AIPState.CREATED);

        // 2. Use archival storage client to post the associated request
        AIPCollection aips = new AIPCollection();
        Iterator<Long> it = aipIds.iterator();
        Set<String> aipsInRequest = Sets.newHashSet();
        while ((aipsInRequest.size() < bulkRequestLimit) && it.hasNext()) {
            Long aipId = it.next();
            AIPEntity aip = aipRepository.findOne(aipId);
            aips.add(aip.getAip());
            aipsInRequest.add(aip.getIpId());
        }
        // Update all aip in request to  AIPState to QUEUED.
        aipsInRequest.forEach(aipId -> aipRepository.updateAIPEntityStateAndErrorMessage(AIPState.QUEUED, aipId, null));
        if (!aipsInRequest.isEmpty()) {
            FeignSecurityManager.asSystem(); // as we are using this method into a schedule, we clearly use the
            ResponseEntity<List<RejectedAip>> response = null;
            try {
                response = aipClient.store(aips);
            } catch (HttpClientErrorException e) {
                // Feign only throws exceptions in case the response status is neither 404 or one of the 2xx,
                // so lets catch the exception and if it not one of our API normal status rethrow it
                if (e.getStatusCode() != HttpStatus.UNPROCESSABLE_ENTITY) {
                    // Response error. Microservice may be not available at the time. Update all AIPs to CREATE state to be handle next time
                    aipsInRequest.forEach(aipId -> aipRepository
                            .updateAIPEntityStateAndErrorMessage(AIPState.CREATED, aipId, null));
                    throw e;
                }
                // first lets get the string from the body then lets deserialize it using gson
                TypeToken<List<RejectedAip>> bodyTypeToken = new TypeToken<List<RejectedAip>>() {

                };
                List<RejectedAip> rejectedAips = gson.fromJson(e.getResponseBodyAsString(), bodyTypeToken.getType());
                //set all aips to store_rejected
                rejectedAips.forEach(rejectedAip -> rejectAip(rejectedAip.getIpId(), rejectedAip.getRejectionCauses()));
            }
            FeignSecurityManager.reset();
            if ((response != null) && (response.getStatusCode().is2xxSuccessful())) {
                List<RejectedAip> rejectedAips = response.getBody();
                // If there is rejected aips, remove them from the list of AIPEntity to set to QUEUED status.
                if ((rejectedAips != null) && !rejectedAips.isEmpty()) {
                    rejectedAips
                            .forEach(rejectedAip -> rejectAip(rejectedAip.getIpId(), rejectedAip.getRejectionCauses()));
                }
            }
        }
    }

    private void rejectAip(String aipId, List<String> rejectionCauses) {
        LOGGER.warn("Created AIP {}, has been rejected by archival storage microservice for store action", aipId);
        StringJoiner errorMessage = new StringJoiner(", ");
        rejectionCauses.forEach(cause -> errorMessage.add(cause));
        setAipInError(aipId, AIPState.STORE_REJECTED, errorMessage.toString());
    }

    @Override
    public void setAipInError(String ipId, AIPState state, String errorMessage) {
        Optional<AIPEntity> oAip = aipRepository.findByIpId(ipId);
        if (oAip.isPresent()) {
            // Update AIP State
            AIPEntity aip = oAip.get();
            aipRepository.updateAIPEntityStateAndErrorMessage(state, ipId, errorMessage);
            // Update SIP associated State
            SIPEntity sip = aip.getSip();
            sip.setState(SIPState.STORE_ERROR);
            sipService.saveSIPEntity(sip);
        }
    }

    @Override
    public void setAipToStored(String ipId) {
        // Retrieve aip and set the new status to stored
        Optional<AIPEntity> oAip = aipRepository.findByIpId(ipId);
        if (oAip.isPresent()) {
            AIPEntity aip = oAip.get();
            aip.setState(AIPState.STORED);
            aip.setErrorMessage(null);
            aipRepository.save(aip);
            // If all AIP are stored update SIP state to STORED
            Set<AIPEntity> sipAips = aipRepository.findBySip(aip.getSip());
            if (sipAips.stream().allMatch(a -> AIPState.STORED.equals(a.getState()))) {
                SIPEntity sip = aip.getSip();
                sip.setState(SIPState.STORED);
                sipService.saveSIPEntity(sip);
                // AIPs are no longer usefull here we can delete them
                aipRepository.delete(sipAips);
            }
        }
    }

    @Override
    public void deleteAip(String ipId, String sipIpId) {
        // Check if deleted AIP exists in internal database
        Optional<AIPEntity> oAip = aipRepository.findByIpId(ipId);
        if (oAip.isPresent()) {
            // Delete aip
            aipRepository.delete(oAip.get());
        }
        // Retrieve all AIP associated to the SIP.
        FeignSecurityManager.asSystem();
        ResponseEntity<PagedResources<Resource<fr.cnes.regards.modules.storage.domain.database.AIPEntity>>> result = aipEntityClient
                .retrieveAIPEntities(sipIpId, 0, 100);
        FeignSecurityManager.reset();
        if (result.getStatusCode().equals(HttpStatus.OK) && (result.getBody() != null)) {
            Optional<SIPEntity> oSip = sipRepository.findOneByIpId(sipIpId);
            if (oSip.isPresent()) {
                SIPEntity sip = oSip.get();
                // If all AIPs are deleted, update sip to DELETED state
                if (result.getBody().getContent().stream()
                        .allMatch(resource -> fr.cnes.regards.modules.storage.domain.AIPState.DELETED
                                .equals(resource.getContent().getState()))) {
                    sip.setState(SIPState.DELETED);
                } else {
                    // Else update sip to incomplete
                    sip.setState(SIPState.INCOMPLETE);
                }
                sip.setLastUpdateDate(OffsetDateTime.now());
                sipService.saveSIPEntity(sip);
            }
        }
    }

}
