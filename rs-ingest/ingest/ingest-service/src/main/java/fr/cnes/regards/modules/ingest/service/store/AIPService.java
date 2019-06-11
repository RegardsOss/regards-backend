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
package fr.cnes.regards.modules.ingest.service.store;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.microservice.maintenance.MaintenanceException;
import fr.cnes.regards.framework.module.rest.utils.HttpUtils;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.entity.SipAIPState;
import fr.cnes.regards.modules.ingest.service.ISIPService;
import fr.cnes.regards.modules.ingest.service.IngestTemplateConfiguration;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.client.IAipEntityClient;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.IAipState;
import fr.cnes.regards.modules.storage.domain.RejectedSip;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import freemarker.template.TemplateException;

/**
 * Service to handle aip related issues in ingest, including sending bulk request of AIP to store to archival storage
 * microservice.
 * @author Sébastien Binda
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
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
    private IJobInfoService jobInfoService;

    @Value("${regards.ingest.aips.bulk.request.limit:1000}")
    private Integer bulkRequestLimit;

    @Autowired
    private IAipClient aipClient;

    @Autowired
    private Gson gson;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private ITemplateService templateService;

    @Override
    public void setAipInError(UniformResourceName aipId, IAipState state, String errorMessage, SIPState sipState) {
        Optional<AIPEntity> oAip = aipRepository.findByAipId(aipId.toString());
        if (oAip.isPresent()) {
            // Update AIP State
            AIPEntity aip = oAip.get();
            aipRepository.updateAIPEntityStateAndErrorMessage(state, aipId.toString(), errorMessage);
            // Update SIP associated State
            SIPEntity sip = aip.getSip();
            sip.setState(sipState);
            // Save the errorMessage inside SIP rejections errors
            sip.getRejectionCauses().add(String.format("Storage of AIP(%s) failed due to the following error: %s",
                                                       aipId,
                                                       errorMessage));
            sipService.saveSIPEntity(sip);
        }
    }

    @Override
    public void setAipToStored(UniformResourceName aipId, IAipState state) {
        // Retrieve aip and set the new status to stored
        Optional<AIPEntity> oAip = aipRepository.findByAipId(aipId.toString());
        if (oAip.isPresent()) {
            AIPEntity aip = oAip.get();
            aip.setState(state);
            aip.setErrorMessage(null);
            aipRepository.save(aip);
            // If all AIP are stored update SIP state to STORED
            Set<AIPEntity> sipAips = aipRepository.findBySip(aip.getSip());
            if (sipAips.stream()
                    .allMatch(a -> AIPState.STORED.equals(a.getState()) || SipAIPState.INDEXED.equals(a.getState()))) {
                SIPEntity sip = aip.getSip();
                sip.setState(SIPState.STORED);
                sipService.saveSIPEntity(sip);
            }
        }
    }

    @Override
    public void deleteAip(UniformResourceName aipId, UniformResourceName sipId, IAipState state) {
        // Check if deleted AIP exists in internal database
        Optional<AIPEntity> oAip = aipRepository.findByAipId(aipId.toString());
        if (oAip.isPresent()) {
            // Delete aip
            aipRepository.delete(oAip.get());
        }
        // Retrieve all AIP associated to the SIP.
        FeignSecurityManager.asSystem();
        ResponseEntity<PagedResources<Resource<fr.cnes.regards.modules.storage.domain.database.AIPEntity>>> result = aipEntityClient
                .retrieveAIPEntities(sipId.toString(), 0, 100);
        FeignSecurityManager.reset();
        if (result.getStatusCode().equals(HttpStatus.OK) && result.getBody() != null) {
            Optional<SIPEntity> oSip = sipRepository.findOneBySipId(sipId.toString());
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

    @Override
    public Optional<AIPEntity> searchAip(UniformResourceName aipId) {
        return aipRepository.findByAipId(aipId.toString());
    }

    @Override
    public AIPEntity setAipToIndexed(AIPEntity aip) {
        aip.setState(SipAIPState.INDEXED);
        aip.setErrorMessage(null);
        aipRepository.save(aip);
        LOGGER.info("AIP \"{}\" is now indexed.", aip.getAipIdUrn().toString());
        // If all AIPs of current SIP are indexed then update SIP state to INDEXED
        Set<AIPEntity> sipAips = aipRepository.findBySip(aip.getSip());
        if (sipAips.stream().allMatch(aipEntity -> aipEntity.getState() == SipAIPState.INDEXED)) {
            SIPEntity sip = aip.getSip();
            sip.setState(SIPState.INDEXED);
            sipService.saveSIPEntity(sip);
            LOGGER.info("SIP \"{}\" is now indexed.", sip.getSipId());
            // AIPs are no longer useful here we can delete them
            aipRepository.deleteAll(sipAips);
        }
        return aip;
    }

    @Override
    public AIPEntity setAipToIndexError(AIPEntity aip) {
        aip.setState(SipAIPState.INDEX_ERROR);
        aip.setErrorMessage(null);
        aipRepository.save(aip);

        LOGGER.info("AIP \"{}\" is now indexed.", aip.getAipIdUrn().toString());
        // If one AIP of current SIP is at INDEX_ERROR than update SIP state to INDEX_ERROR
        Set<AIPEntity> sipAips = aipRepository.findBySip(aip.getSip());
        if (sipAips.stream().anyMatch(aipEntity -> aipEntity.getState() == SipAIPState.INDEX_ERROR)) {
            SIPEntity sip = aip.getSip();
            sip.setState(SIPState.INDEX_ERROR);
            sipService.saveSIPEntity(sip);
            LOGGER.info("SIP \"{}\" cannot be indexed.", sip.getSipId());
        }
        return aip;
    }

    @Override
    public void handleJobEvent(JobEvent jobEvent) {
        if (JobEventType.FAILED.equals(jobEvent.getJobEventType())) {
            // Load job info
            @SuppressWarnings("unused") JobInfo jobInfo = jobInfoService.retrieveJob(jobEvent.getJobId());
            String jobClass = jobInfo.getClassName();
            String title = "Unhandled job error";
            LOGGER.warn(title + String.format(" %s/%s", jobClass, jobInfo.getId().toString()));
            String stacktrace = jobInfo.getStatus().getStackTrace();
            LOGGER.warn(stacktrace);
            notificationClient.notify(stacktrace, title, NotificationLevel.ERROR, DefaultRole.ADMIN);
        }
    }

    @Override
    public void handleAipEvent(AIPEvent aipEvent) {
        UniformResourceName aipId = UniformResourceName.fromString(aipEvent.getAipId());
        LOGGER.info("[AIP Event received] {} - {} - {}",
                    aipEvent.getAipId(),
                    aipEvent.getAipState(),
                    aipEvent.getFailureCause() != null ? aipEvent.getFailureCause() : "ok");
        switch (aipEvent.getAipState()) {
            case STORAGE_ERROR:
                setAipInError(aipId, aipEvent.getAipState(), aipEvent.getFailureCause(), SIPState.STORE_ERROR);
                break;
            case STORED:
                setAipToStored(aipId, aipEvent.getAipState());
                break;
            case DELETED:
                deleteAip(aipId, UniformResourceName.fromString(aipEvent.getSipId()), aipEvent.getAipState());
                break;
            case PENDING:
            case STORING_METADATA:
            case VALID:
            default:
                break;
        }
    }

    @Override
    public AIPEntity save(AIPEntity entity) {
        return aipRepository.save(entity);
    }

    @Override
    public void askForAipsDeletion() {
        List<RejectedSip> rejectedSips = new ArrayList<>();
        Page<SIPEntity> deletableSips = sipRepository.findPageByState(SIPState.TO_BE_DELETED, PageRequest.of(0, 100));
        if (deletableSips.hasContent()) {
            ResponseEntity<List<RejectedSip>> response;
            long askForAipDeletionStart = System.currentTimeMillis();
            FeignSecurityManager.asSystem();
            try {
                response = aipClient.deleteAipFromSips(deletableSips.getContent().stream().map(SIPEntity::getSipId)
                                                               .collect(Collectors.toSet()));
                long askForAipDeletionEnd = System.currentTimeMillis();
                LOGGER.trace("Asking SUCCESSFULLY for storage to delete {} sip took {} ms",
                             deletableSips.getNumberOfElements(),
                             askForAipDeletionEnd - askForAipDeletionStart);
                if (HttpUtils.isSuccess(response.getStatusCode())) {
                    if (response.getBody() != null) {
                        rejectedSips = response.getBody();
                    }
                }
            } catch (HttpClientErrorException e) {
                // Feign only throws exceptions in case the response status is neither 404 or one of the 2xx,
                // so lets catch the exception and if it not one of our API normal status rethrow it
                if (e.getStatusCode() != HttpStatus.UNPROCESSABLE_ENTITY) {
                    throw e;
                }
                // first lets get the string from the body then lets deserialize it using gson
                @SuppressWarnings("serial")
                TypeToken<List<RejectedSip>> bodyTypeToken = new TypeToken<List<RejectedSip>>() {

                };
                rejectedSips = gson.fromJson(e.getResponseBodyAsString(), bodyTypeToken.getType());
            } finally {
                long askForAipDeletionEnd = System.currentTimeMillis();
                LOGGER.trace("Asking for storage to delete {} sip took {} ms",
                             deletableSips.getNumberOfElements(),
                             askForAipDeletionEnd - askForAipDeletionStart);
                FeignSecurityManager.reset();
            }
            FeignSecurityManager.asSystem();
            try {
                if (!rejectedSips.isEmpty()) {
                    sendRejectedSipNotification(rejectedSips);
                }
            } catch (HttpClientErrorException ce) {
                LOGGER.error("Could not send notification because of client side error.", ce);
                // probably a development error or version compatibility issue so lets rethrow the exception
                throw new RsRuntimeException(ce);
            } catch (HttpServerErrorException se) {
                LOGGER.error("Could not send notification because of server side error. Check rs-admin logs.", se);
            } finally {
                FeignSecurityManager.reset();
            }
            // set state to deleted
            Set<String> rejectedSipIds = rejectedSips.stream().map(RejectedSip::getSipId).collect(Collectors.toSet());
            for (SIPEntity sip : deletableSips.getContent()) {
                if (!rejectedSipIds.contains(sip.getSipId())) {
                    sip.setState(SIPState.DELETED);
                    sipRepository.save(sip);
                }
            }
        }
    }

    /**
     * Be aware that this method does not touch to FeignSecurityManager.
     * Be sure to handle FeignSecurityManager issues before and after calling this method.
     */
    private void sendRejectedSipNotification(List<RejectedSip> rejectedSips) {
        // lets prepare the notification message
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("rejectedSips", rejectedSips);
        // lets use the template service to get our message
        String message;
        try {
            message = templateService.render(IngestTemplateConfiguration.REJECTED_SIPS_TEMPLATE_NAME, dataMap);
        } catch (TemplateException e) {
            throw new MaintenanceException(e.getMessage(), e);
        }
        notificationClient.notify(message, "Errors during SIPs deletions", NotificationLevel.ERROR, DefaultRole.ADMIN);
    }
}
