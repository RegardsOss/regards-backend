/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.aip;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;
import fr.cnes.regards.modules.templates.service.ITemplateService;

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
    private IAIPRepository aipRepository;

    @Autowired
    private IJobInfoService jobInfoService;

    @Value("${regards.ingest.aips.bulk.request.limit:1000}")
    private Integer bulkRequestLimit;

    @Autowired
    private Gson gson;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private ITemplateService templateService;

    @Override
    public List<AIPEntity> createAndSave(SIPEntity sip, List<AIP> aips) {
        List<AIPEntity> entities = new ArrayList<>();
        for (AIP aip : aips) {
            entities.add(aipRepository.save(AIPEntity.build(sip, AIPState.CREATED, aip)));
        }
        return entities;
    }

    @Override
    public void setAipInError(UniformResourceName aipId, AIPState state, String errorMessage, SIPState sipState) {
        Optional<AIPEntity> oAip = aipRepository.findByAipId(aipId.toString());
        if (oAip.isPresent()) {
            // Update AIP State
            AIPEntity aip = oAip.get();
            aipRepository.updateAIPEntityStateAndErrorMessage(state, aipId.toString(), errorMessage);
            // Update SIP associated State
            SIPEntity sip = aip.getSip();
            sip.setState(sipState);
            //            // Save the errorMessage inside SIP rejections errors
            //            sip.getRejectionCauses().add(String.format("Storage of AIP(%s) failed due to the following error: %s",
            //                                                       aipId, errorMessage));
            sipService.saveSIPEntity(sip);
        }
    }

    @Override
    public void setAipToStored(UniformResourceName aipId, AIPState state) {
        // Retrieve aip and set the new status to stored
        Optional<AIPEntity> oAip = aipRepository.findByAipId(aipId.toString());
        if (oAip.isPresent()) {
            AIPEntity aip = oAip.get();
            aip.setState(state);
            aip.setErrorMessage(null);
            aipRepository.save(aip);
        }
    }

    @Override
    public void deleteAip(UniformResourceName aipId, UniformResourceName sipId, AIPState state) {
        // FIXME : refactor
        // Check if deleted AIP exists in internal database
        //        Optional<AIPEntity> oAip = aipRepository.findByAipId(aipId.toString());
        //        if (oAip.isPresent()) {
        //            // Delete aip
        //            aipRepository.delete(oAip.get());
        //        }
        //        // Retrieve all AIP associated to the SIP.
        //        FeignSecurityManager.asSystem();
        //        ResponseEntity<PagedResources<Resource<fr.cnes.regards.modules.storage.domain.database.AIPEntity>>> result = aipEntityClient
        //                .retrieveAIPEntities(sipId.toString(), 0, 100);
        //        FeignSecurityManager.reset();
        //        if (result.getStatusCode().equals(HttpStatus.OK) && result.getBody() != null) {
        //            Optional<SIPEntity> oSip = sipRepository.findOneBySipId(sipId.toString());
        //            if (oSip.isPresent()) {
        //                SIPEntity sip = oSip.get();
        //                // If all AIPs are deleted, update sip to DELETED state
        //                if (result.getBody().getContent().stream()
        //                        .allMatch(resource -> fr.cnes.regards.modules.storage.domain.AIPState.DELETED
        //                                .equals(resource.getContent().getState()))) {
        //                    sip.setState(SIPState.DELETED);
        //                } else {
        //                    // Else update sip to incomplete
        //                    // FIXME
        //                    // sip.setState(SIPState.INCOMPLETE);
        //                }
        //                sip.setLastUpdateDate(OffsetDateTime.now());
        //                sipService.saveSIPEntity(sip);
        //            }
        //        }
    }

    @Override
    public Optional<AIPEntity> searchAip(UniformResourceName aipId) {
        return aipRepository.findByAipId(aipId.toString());
    }

    @Override
    public void handleJobEvent(JobEvent jobEvent) {
        if (JobEventType.FAILED.equals(jobEvent.getJobEventType())) {
            // Load job info
            @SuppressWarnings("unused")
            JobInfo jobInfo = jobInfoService.retrieveJob(jobEvent.getJobId());
            String jobClass = jobInfo.getClassName();
            String title = "Unhandled job error";
            LOGGER.warn(title + String.format(" %s/%s", jobClass, jobInfo.getId().toString()));
            String stacktrace = jobInfo.getStatus().getStackTrace();
            LOGGER.warn(stacktrace);
            notificationClient.notify(stacktrace, title, NotificationLevel.ERROR, DefaultRole.ADMIN);
        }
    }

    @Override
    public AIPEntity save(AIPEntity entity) {
        return aipRepository.save(entity);
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.ingest.service.store.IAIPService#askForAipsDeletion()
     */
    @Override
    public void askForAipsDeletion() {
        // TODO Auto-generated method stub
        // TODO refactor with files only
    }
}
