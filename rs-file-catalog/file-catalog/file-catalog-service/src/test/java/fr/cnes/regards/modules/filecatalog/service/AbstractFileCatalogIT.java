/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.filecatalog.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.fileaccess.dto.FileArchiveStatus;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.modules.fileaccess.client.IStorageLocationConfigurationClient;
import fr.cnes.regards.modules.filecatalog.dao.*;
import fr.cnes.regards.modules.filecatalog.domain.FileLocation;
import fr.cnes.regards.modules.filecatalog.domain.FileReference;
import fr.cnes.regards.modules.filecatalog.domain.FileReferenceMetaInfo;
import fr.cnes.regards.modules.filecatalog.service.handler.FileArchiveResponseEventHandler;
import fr.cnes.regards.modules.filecatalog.service.location.StorageLocationService;
import org.junit.Assert;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.*;

/**
 * @author Thibaud Michaudel
 **/
public abstract class AbstractFileCatalogIT extends AbstractMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMultitenantServiceIT.class);

    protected static final String NEARLINE_CONF_LABEL = "NL_target";

    protected static final String ONLINE_CONF_LABEL = "target";

    protected static final String OFFLINE_CONF_LABEL = "offline";

    @Autowired
    protected FileReferenceService fileReferenceService;

    @Autowired
    protected FileReferenceRequestService fileRefeferenceRequestservice;

    @Autowired
    protected FileStorageRequestService fileStorageRequestService;

    @Autowired
    protected IFileDeletionRequestRepository fileDeletionRequestRepository;

    @Autowired
    protected IFileReferenceRepository fileReferenceRepository;

    @Autowired
    protected IFileReferenceWithOwnersRepository fileReferenceWithOwnersRepository;

    @Autowired
    protected IFileStorageRequestAggregationRepository fileStorageRequestAggregationRepository;

    @Autowired
    protected RequestResultInfoRepository requestResultInfoRepository;

    @Autowired
    protected IRequestGroupRepository requestGroupRepository;

    @Autowired
    protected IStorageLocationRepository storageLocationRepo;

    @Autowired
    protected StorageLocationService storageLocationService;

    @MockBean
    protected IStorageLocationConfigurationClient storageLocationConfigurationClient;

    @Autowired
    protected FileArchiveResponseEventHandler fileArchiveResponseEventHandler;

    protected void init() throws ModuleException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        simulateApplicationStartedEvent();
        fileDeletionRequestRepository.deleteAll();
        requestResultInfoRepository.deleteAll();
        fileReferenceRepository.deleteAll();
        fileStorageRequestAggregationRepository.deleteAll();
        requestGroupRepository.deleteAll();
        storageLocationRepo.deleteAll();
        storageLocationService.invalidateCache();
        Mockito.clearInvocations(publisher);
    }

    protected Optional<FileReference> referenceFile(String checksum,
                                                    String owner,
                                                    String type,
                                                    String fileName,
                                                    String storage,
                                                    String sessionOwner,
                                                    String session,
                                                    FileArchiveStatus fileArchiveStatus) {
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum,
                                                                       "MD5",
                                                                       fileName,
                                                                       1024L,
                                                                       MediaType.APPLICATION_OCTET_STREAM);
        fileMetaInfo.setType(type);
        FileLocation location = new FileLocation(storage, "anywhere://in/this/directory/file.test", fileArchiveStatus);
        try {
            fileRefeferenceRequestservice.reference(owner,
                                                    fileMetaInfo,
                                                    location,
                                                    Sets.newHashSet(UUID.randomUUID().toString()),
                                                    sessionOwner,
                                                    session);
        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
        return fileReferenceService.search(location.getStorage(), fileMetaInfo.getChecksum());
    }

    protected Optional<FileReference> referenceRandomFile(String owner,
                                                          String type,
                                                          String fileName,
                                                          String storage,
                                                          String sessionOwner,
                                                          String session,
                                                          FileArchiveStatus fileArchiveStatus) {
        return this.referenceFile(UUID.randomUUID().toString(),
                                  owner,
                                  type,
                                  fileName,
                                  storage,
                                  sessionOwner,
                                  session,
                                  fileArchiveStatus);
    }

    protected List<StepPropertyUpdateRequestEvent> getStepPropertyEvents(Collection<ISubscribable> events) {
        // get all events of type StepPropertyUpdateRequestEvent
        List<StepPropertyUpdateRequestEvent> stepList = new ArrayList<>();
        for (ISubscribable e : events) {
            if (e instanceof StepPropertyUpdateRequestEvent) {
                stepList.add((StepPropertyUpdateRequestEvent) e);
            }
        }
        // sort list to make sure it is sorted by creation date
        stepList.sort(Comparator.comparing(StepPropertyUpdateRequestEvent::getDate));
        return stepList;
    }

    protected void checkStepEvent(StepPropertyUpdateRequestEvent event,
                                  SessionNotifierPropertyEnum expectedEventProperty,
                                  StepPropertyEventTypeEnum expectedType,
                                  String expectedSessionOwner,
                                  String expectedSession,
                                  String expectedValue) {
        StepProperty stepProperty = event.getStepProperty();
        Assert.assertEquals("This property was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedEventProperty.getName(),
                            stepProperty.getStepPropertyInfo().getProperty());
        Assert.assertEquals("This value was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedValue,
                            stepProperty.getStepPropertyInfo().getValue());
        Assert.assertEquals("This type was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedType,
                            event.getType());
        Assert.assertEquals("This session owner was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedSessionOwner,
                            stepProperty.getSource());
        Assert.assertEquals("This session was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedSession,
                            stepProperty.getSession());
    }
}
