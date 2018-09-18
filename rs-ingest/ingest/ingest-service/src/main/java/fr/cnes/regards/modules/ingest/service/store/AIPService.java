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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.microservice.maintenance.MaintenanceException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.utils.HttpUtils;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.entity.SipAIPState;
import fr.cnes.regards.modules.ingest.service.ISIPService;
import fr.cnes.regards.modules.ingest.service.chain.IIngestProcessingService;
import fr.cnes.regards.modules.ingest.service.job.AIPSubmissionJob;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.notification.domain.NotificationType;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.client.IAipEntityClient;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.IAipState;
import fr.cnes.regards.modules.storage.domain.RejectedSip;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import fr.cnes.regards.modules.templates.service.TemplateServiceConfiguration;

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

    @SuppressWarnings("unused")
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
    private IIngestProcessingService processingService;

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
    public void setAipInError(UniformResourceName aipId, IAipState state, String errorMessage) {
        Optional<AIPEntity> oAip = aipRepository.findByAipId(aipId.toString());
        if (oAip.isPresent()) {
            // Update AIP State
            AIPEntity aip = oAip.get();
            aipRepository.updateAIPEntityStateAndErrorMessage(state, aipId.toString(), errorMessage);
            // Update SIP associated State
            SIPEntity sip = aip.getSip();
            sip.setState(SIPState.STORE_ERROR);
            // Save the errorMessage inside SIP rejections errors
            sip.getRejectionCauses()
                    .add(String.format("Storage of AIP(%s) failed due to the following error: %s", aipId,
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
        if (result.getStatusCode().equals(HttpStatus.OK) && (result.getBody() != null)) {
            Optional<SIPEntity> oSip = sipRepository.findOneBySipId(sipId.toString());
            if (oSip.isPresent()) {
                SIPEntity sip = oSip.get();
                // If all AIPs are deleted, update sip to DELETED state
                if (result.getBody().getContent().stream().allMatch(
                        resource -> fr.cnes.regards.modules.storage.domain.AIPState.DELETED
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
            aipRepository.delete(sipAips);
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

    /**
     * This method is called by a time scheduler. We only schedule on job per ingest chain if and
     * only if an existing job not already exists. To detect that a job is already scheduled, we check the AIP state of
     * the chain. AIPs not already scheduled will be scheduled on next scheduler call.
     */
    @Override
    public void scheduleAIPStorageBulkRequest() {
        // Find all processing chains
        List<IngestProcessingChain> ipcs = processingService.findAll();
        // For each processing chain
        for (IngestProcessingChain ipc : ipcs) {
            // Check if submission not already scheduled
            if (!aipRepository.isAlreadyWorking(ipc.getName())) {
                Page<AIPEntity> page = aipRepository
                        .findWithLockBySipProcessingAndState(ipc.getName(), SipAIPState.CREATED,
                                                             new PageRequest(0, bulkRequestLimit));
                if (page.hasContent()) {
                    // Schedule AIP page submission
                    for (AIPEntity aip : page.getContent()) {
                        aip.setState(SipAIPState.SUBMISSION_SCHEDULED);
                        aipRepository.save(aip);
                    }
                    // Schedule job
                    Set<JobParameter> jobParameters = Sets.newHashSet();
                    jobParameters.add(new JobParameter(AIPSubmissionJob.INGEST_CHAIN_PARAMETER, ipc.getName()));

                    JobInfo jobInfo = new JobInfo(false);
                    jobInfo.setPriority(IngestJobPriority.AIP_SUBMISSION_JOB_PRIORITY.getPriority());
                    jobInfo.setParameters(jobParameters);
                    jobInfo.setClassName(AIPSubmissionJob.class.getName());
                    jobInfoService.createAsQueued(jobInfo);
                }
            }
        }
    }

    @Override
    public void handleJobEvent(JobEvent jobEvent) {
        if (JobEventType.FAILED.equals(jobEvent.getJobEventType())) {
            // Load job info
            JobInfo jobInfo = jobInfoService.retrieveJob(jobEvent.getJobId());
            handleAIPSubmissiontError(jobInfo);
        }
    }

    private void handleAIPSubmissiontError(JobInfo jobInfo) {
        if (AIPSubmissionJob.class.getName().equals(jobInfo.getClassName())) {
            Map<String, JobParameter> params = jobInfo.getParametersAsMap();
            String ingestChain = params.get(AIPSubmissionJob.INGEST_CHAIN_PARAMETER).getValue();
            // Set to submission error
            Set<AIPEntity> aips = aipRepository
                    .findBySipProcessingAndState(ingestChain, SipAIPState.SUBMISSION_SCHEDULED);
            for (AIPEntity aip : aips) {
                setAipInError(aip.getAipIdUrn(), SipAIPState.SUBMISSION_ERROR, "Submission job error");
            }
        }
    }

    @Override
    public void handleAipEvent(AIPEvent aipEvent) {
        UniformResourceName aipId = UniformResourceName.fromString(aipEvent.getAipId());
        switch (aipEvent.getAipState()) {
            case STORAGE_ERROR:
                setAipInError(aipId, aipEvent.getAipState(), aipEvent.getFailureCause());
                break;
            case STORED:
                setAipToStored(aipId, aipEvent.getAipState());
                break;
            case DELETED:
                deleteAip(aipId, UniformResourceName.fromString(aipEvent.getSipId()), aipEvent.getAipState());
                break;
            case PENDING:
            case STORING_METADATA:
            case UPDATED:
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
    public Set<AIPEntity> findAIPToSubmit(String ingestProcessingChain) {
        return aipRepository.findBySipProcessingAndState(ingestProcessingChain, SipAIPState.SUBMISSION_SCHEDULED);
    }

    @Override
    public void askForAipsDeletion() {
        List<RejectedSip> rejectedSips = new ArrayList<>();
        Page<SIPEntity> deletableSips = sipRepository.findPageByState(SIPState.TO_BE_DELETED, new PageRequest(0, 100));
        if (deletableSips.hasContent()) {
            ResponseEntity<List<RejectedSip>> response;
            long askForAipDeletionStart = System.currentTimeMillis();
            FeignSecurityManager.asSystem();
            try {
                response = aipClient.deleteAipFromSips(
                        deletableSips.getContent().stream().map(SIPEntity::getSipId).collect(Collectors.toSet()));
                long askForAipDeletionEnd = System.currentTimeMillis();
                LOGGER.trace("Asking SUCCESSFULLY for storage to delete {} sip took {} ms",
                             deletableSips.getNumberOfElements(), askForAipDeletionEnd - askForAipDeletionStart);
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
                LOGGER.trace("Asking for storage to delete {} sip took {} ms", deletableSips.getNumberOfElements(),
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
        SimpleMailMessage email;
        try {
            email = templateService.writeToEmail(TemplateServiceConfiguration.REJECTED_SIPS_CODE, dataMap);
        } catch (EntityNotFoundException enf) {
            throw new MaintenanceException(enf.getMessage(), enf);
        }
        notificationClient
                .notifyRoles(email.getText(), "Errors during SIPs deletions", "rs-ingest", NotificationType.ERROR,
                             DefaultRole.ADMIN);
    }
}
