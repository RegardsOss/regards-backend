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
package fr.cnes.regards.modules.ingest.service.dump;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.dump.service.DumpService;
import fr.cnes.regards.framework.modules.dump.service.ObjectDump;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPSaveMetadataRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.exception.DuplicateUniqueNameException;
import fr.cnes.regards.modules.ingest.domain.exception.NothingToDoException;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.dump.AIPSaveMetadataRequest;

/**
 * see {@link IAIPMetadataService}
 * @author Iliana Ghazali
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@MultitenantTransactional
public class AIPMetadataService implements IAIPMetadataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPMetadataService.class);

    // Limit number of AIPs to retrieve in one page
    @Value("${regards.aip.dump.zip-limit:1000}")
    private int zipLimit;

    @Autowired
    private IAIPSaveMetadataRequestRepository metadataRequestRepository;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private DumpService dumpService;

    @Autowired
    private IAIPMetadataService self;

    @Autowired
    private INotificationClient notificationClient;

    @Override
    public void writeDump(AIPSaveMetadataRequest metadataRequest, Path dumpLocation, Path tmpZipLocation)
            throws IOException {
        // Write dump (create a zip of multiple zips)
        dumpService.generateDump(dumpLocation, tmpZipLocation, metadataRequest.getCreationDate());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void writeZips(AIPSaveMetadataRequest metadataRequest, Path tmpZipLocation)
            throws NothingToDoException, IOException {
        Pageable pageToRequest = PageRequest.of(0, zipLimit, Sort.by(Sort.Order.asc("creationDate")));
        try {
            do {
                // zip json files generated from aips
                pageToRequest = self.dumpOnePage(metadataRequest, pageToRequest, tmpZipLocation);
            } while (pageToRequest != null);
        } catch (DuplicateUniqueNameException e) {
            self.handleError(metadataRequest, e.getMessage());
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public Pageable dumpOnePage(AIPSaveMetadataRequest metadataRequest, Pageable pageToRequest, Path tmpZipLocation)
            throws IOException, DuplicateUniqueNameException, NothingToDoException {
        // Find aips to zip
        Page<AIPEntity> aipToDump;
        OffsetDateTime previousDumpDate = metadataRequest.getPreviousDumpDate();
        OffsetDateTime dumpDate = metadataRequest.getCreationDate();
        // If previousDumpDate is null, find all aips with lastUpdate < previousDumpDate
        if (previousDumpDate == null) {
            aipToDump = aipRepository.findByLastUpdateLessThan(dumpDate, pageToRequest);
        } else {
            // else find all aips with previousDumpDate < lastUpdate < dumpDate
            aipToDump = aipRepository.findByLastUpdateBetween(previousDumpDate, dumpDate, pageToRequest);
        }

        // If no aip was found, throw NothingToDoException
        if (!aipToDump.hasContent()) {
            throw new NothingToDoException(
                    String.format("There is nothing to dump between %s and %s", previousDumpDate, dumpDate));
        }

        // If some aips were found, convert them into ObjectDump
        List<ObjectDump> objectDumps = convertAipToObjectDump(aipToDump.getContent());

        // Check if json names <providerId>-<version> are unique in the collection
        // If yes, throw DuplicateUniqueNameException
        List<ObjectDump> duplicatedJsonNames;
        duplicatedJsonNames = dumpService.checkUniqueJsonNames(objectDumps);
        if (!duplicatedJsonNames.isEmpty()) {
            String errorMessage = duplicatedJsonNames.stream().map(ObjectDump::getJsonName).collect(Collectors.joining(
                    ", ", "Some AIPs to dump had the same generated names "
                            + "(providerId-version.json) should be unique: ",
                    ". Please edit your AIPs so there is no duplicates."));
            handleError(metadataRequest, errorMessage);
            throw new DuplicateUniqueNameException(errorMessage);
        }

        // If no error was detected, create zip that contains json files generated from aips
        try {
            dumpService.generateJsonZip(objectDumps, tmpZipLocation);
        } catch (IOException e) {
            LOGGER.error("Error while dumping one page of aip", e);
            throw e;
        }
        return aipToDump.hasNext() ? aipToDump.nextPageable() : null;
    }

    private List<ObjectDump> convertAipToObjectDump(Collection<AIPEntity> aipEntities) {
        return aipEntities.stream().map(aipEntity -> new ObjectDump(aipEntity.getCreationDate(),
                                                                    aipEntity.getProviderId() + "-" + aipEntity
                                                                            .getVersion(), aipEntity.getAip(),
                                                                    aipEntity.getAipId())).collect(Collectors.toList());
    }

    @Override
    public void handleError(AIPSaveMetadataRequest metadataRequest, String errorMessage) {
        notificationClient.notify(errorMessage, String.format("Error while dumping AIPs for period %s to %s",
                                                              metadataRequest.getPreviousDumpDate(),
                                                              metadataRequest.getCreationDate()),
                                  NotificationLevel.ERROR, DefaultRole.ADMIN);
        metadataRequest.addError(errorMessage);
        metadataRequest.setState(InternalRequestState.ERROR);
        metadataRequestRepository.save(metadataRequest);
        // no need to clean up workspace as job service is doing so
    }

    @Override
    public void handleSuccess(AIPSaveMetadataRequest metadataRequest) {
        metadataRequestRepository.delete(metadataRequest);
        // we do not need to clean up workspace as job service is doing so for us
    }

}
