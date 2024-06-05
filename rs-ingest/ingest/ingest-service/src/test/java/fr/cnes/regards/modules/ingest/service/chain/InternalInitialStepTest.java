/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.chain;

import fr.cnes.regards.framework.modules.jobs.domain.step.ProcessingStepException;
import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import fr.cnes.regards.modules.ingest.dto.AIPState;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
import fr.cnes.regards.modules.ingest.service.chain.step.InternalInitialStep;
import fr.cnes.regards.modules.ingest.service.job.IngestProcessingJob;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import software.amazon.awssdk.utils.Md5Utils;

import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Sébastien Binda
 **/
@RunWith(SpringRunner.class)
@Profile({ "nojob", "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingest_initial_step" },
                    locations = { "classpath:application-test.properties" })
public class InternalInitialStepTest extends IngestMultitenantServiceIT {

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Test
    @Purpose("A new ingest request with replace error activated should delete old request/sip/aip in error state for "
             + "the same providerId")
    public void test_with_replace_errors() throws ProcessingStepException {
        test_replace_error_mode(true);
    }

    @Test
    @Purpose("A new ingest request with replace error not activated should keep old request/sip/aip in error state "
             + "for the same providerId")
    public void test_without_replace_errors() throws ProcessingStepException {
        test_replace_error_mode(false);
    }

    private void test_replace_error_mode(boolean replaceMode) throws ProcessingStepException {
        IngestProcessingJob jobMock = Mockito.mock(IngestProcessingJob.class);
        IngestProcessingChain chain = Mockito.mock(IngestProcessingChain.class);
        InternalInitialStep step = new InternalInitialStep(jobMock, chain);
        beanFactory.autowireBean(step);

        // Init two error requests in db (with sip and aip associated)
        initIngestRequest("providerId_1",
                          InternalRequestState.ERROR,
                          IngestRequestStep.REMOTE_STORAGE_ERROR,
                          1,
                          "content_1",
                          false);
        initIngestRequest("providerId_2",
                          InternalRequestState.ERROR,
                          IngestRequestStep.REMOTE_STORAGE_ERROR,
                          1,
                          "content_2",
                          false);

        // Init a new request for same provider id with RUNNING state
        IngestRequest newRequest = initIngestRequest("providerId_1",
                                                     InternalRequestState.RUNNING,
                                                     IngestRequestStep.LOCAL_INIT,
                                                     null,
                                                     "content_1",
                                                     replaceMode);

        Mockito.when(jobMock.getCurrentRequest()).thenReturn(newRequest);

        // Run InternalInitialStep
        SIPEntity sip = step.execute(newRequest);

        // Then check resulting number of request/sip/aip
        // If replaceMode is activated, old error request with the same providerId should be deleted
        Assert.assertEquals(replaceMode ? 2 : 3, ingestRequestRepository.findAll().size());
        Assert.assertEquals(replaceMode ? 1 : 2, aipRepository.findAll().size());
        Assert.assertEquals(replaceMode ? 1 : 2, sipRepository.findAll().size());
        Assert.assertEquals(replaceMode ? 1 : 2, sip.getVersion().intValue());
    }

    private IngestRequest initIngestRequest(String providerId,
                                            InternalRequestState state,
                                            IngestRequestStep step,
                                            @Nullable Integer version,
                                            String sipContent,
                                            boolean replaceErrors) {
        IngestRequest ingestRequest = createIngestRequest(providerId, state, step, replaceErrors);
        if (state == InternalRequestState.ERROR) {
            // Init sip in DB
            SIPEntity sipEntity = createSIPEntity(ingestRequest.getMetadata(),
                                                  ingestRequest.getSip(),
                                                  version,
                                                  sipContent);

            // Init aip in DB
            if (step == IngestRequestStep.REMOTE_STORAGE_ERROR) {
                ingestRequest.setAips(List.of(createAIPEntity(sipEntity, providerId)));
                ingestRequest = ingestRequestRepository.save(ingestRequest);
            }
        }
        return ingestRequest;
    }

    private IngestRequest createIngestRequest(String providerId,
                                              InternalRequestState state,
                                              IngestRequestStep step,
                                              boolean replaceErrors) {
        SIPDto sip = SIPDto.build(EntityType.DATA, providerId);
        IngestMetadata metadata = IngestMetadata.build("owner",
                                                       "session",
                                                       OffsetDateTime.now(),
                                                       "chain",
                                                       new HashSet<>(),
                                                       "model",
                                                       StorageMetadata.build("Local"));
        metadata.setReplaceErrors(replaceErrors);
        IngestRequest request = IngestRequest.build(UUID.randomUUID().toString(), metadata, state, step, sip);
        return ingestRequestRepository.save(request);
    }

    private SIPEntity createSIPEntity(IngestMetadata metadata, SIPDto sip, Integer version, String sipContent) {
        SIPEntity sipEntity = SIPEntity.build("tenant", metadata, sip, version, SIPState.INGESTED);
        sipEntity.setLastUpdate(OffsetDateTime.now());
        sipEntity.setChecksum(Md5Utils.md5AsBase64(sipContent.getBytes()));
        return sipRepository.save(sipEntity);
    }

    private AIPEntity createAIPEntity(SIPEntity sipEntity, String providerId) {
        Integer version = sipEntity.getVersion() == null ? 1 : sipEntity.getVersion();
        OaisUniformResourceName urn = OaisUniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP,
                                                                              EntityType.DATA,
                                                                              "tenant",
                                                                              version);
        AIPDto aip = AIPDto.build(sipEntity.getSip(), urn, Optional.of(sipEntity.getSipIdUrn()), providerId, version);
        AIPEntity aipEntity = AIPEntity.build(AIPState.GENERATED, aip);
        aipEntity.setVersion(version);
        aipEntity.setProviderId(aip.getProviderId());
        aipEntity.setSession(sipEntity.getSession());
        aipEntity.setSessionOwner(sipEntity.getSessionOwner());
        aipEntity.setSip(sipEntity);
        aipEntity.setCategories(new HashSet<>());
        return aipRepository.save(aipEntity);
    }

}
