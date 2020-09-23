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
package fr.cnes.regards.modules.ingest.service.aip;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

import fr.cnes.regards.framework.dump.DumpService;
import fr.cnes.regards.framework.dump.ObjectDump;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.ingest.dao.IDumpConfigurationRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPSaveMetadataRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.dump.DumpConfiguration;
import fr.cnes.regards.modules.ingest.domain.exception.DuplicateUniqueNameException;
import fr.cnes.regards.modules.ingest.domain.exception.NothingToDoException;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.dump.AIPSaveMetadataRequest;

/**
 * Service to dump aips
 * @author Iliana Ghazali
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@MultitenantTransactional
public class AIPMetadataService implements IAIPMetadataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPMetadataService.class);

    // Limit number of AIPs to retrieve in one page
    @Value("${regards.dump.zip-limit:1000}")
    private int zipLimit;

    @Autowired
    private IAIPSaveMetadataRequestRepository metadataRequestRepository;

    @Autowired
    private IDumpConfigurationRepository dumpRepository;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private DumpService dumpService;

    @Autowired
    private IAIPMetadataService self;

    @Autowired
    private INotificationClient notificationClient;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void writeZips(AIPSaveMetadataRequest metadataRequest, Path tmpZipLocation)
            throws NothingToDoException {
        Pageable pageToRequest = PageRequest.of(0, zipLimit, Sort.by(Sort.Order.asc("creationDate")));
        try {
            do {
                pageToRequest = self.dumpOnePage(metadataRequest, pageToRequest, tmpZipLocation);
            } while (pageToRequest != null);
        } catch (IOException e) {
            String errorMessage = e.getClass().getSimpleName() + " " + e.getMessage();
            self.handleError(metadataRequest, errorMessage);
            throw new RsRuntimeException(errorMessage, e);
        } catch (DuplicateUniqueNameException e) {
            self.handleError(metadataRequest, e.getMessage());
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public void writeDump(AIPSaveMetadataRequest metadataRequest, Path dumpLocation, Path tmpZipLocation) {
        OffsetDateTime creationDate = metadataRequest.getCreationDate();
        try {
            // Write dump
            dumpService.generateDump(dumpLocation, tmpZipLocation, creationDate);
        } catch (IOException e) {
            LOGGER.error("Error while writing aip dump", e);
            throw new RsRuntimeException(e.getClass().getSimpleName() + " " + e.getMessage(), e);
        }
    }

    @Override
    public Pageable dumpOnePage(AIPSaveMetadataRequest metadataRequest, Pageable pageToRequest,
            Path workspace) throws IOException, DuplicateUniqueNameException, NothingToDoException {
        // Write Zips
        List<ObjectDump> objectDumps;
        List<ObjectDump> duplicatedJsonNames;

        Page<AIPEntity> aipToDump = null;
        OffsetDateTime previousDumpDate = metadataRequest.getPreviousDumpDate();
        OffsetDateTime dumpDate = metadataRequest.getCreationDate();
        if (previousDumpDate == null) {
            aipToDump = aipRepository.findByLastUpdateLessThan(dumpDate, pageToRequest);
        } else {
            aipToDump = aipRepository.findByLastUpdateBetween(previousDumpDate, dumpDate, pageToRequest);
        }

        if (!aipToDump.hasContent()) {
            throw new NothingToDoException(
                    String.format("There is nothing to dump between %s and %s", previousDumpDate, dumpDate));
        }

        // Convert objects
        objectDumps = convertAipToObjectDump(aipToDump.getContent());

        // Check if names are unique in the collection
        duplicatedJsonNames = dumpService.checkUniqueJsonNames(objectDumps);
        if (!duplicatedJsonNames.isEmpty()) {
            String errorMessage = duplicatedJsonNames.stream().map(ObjectDump::getJsonName).collect(Collectors.joining(
                    ", ", "Some AIPs to dump had same generated names "
                            + "(providerId-version.json) which should be unique: ",
                    ". Please edit your AIPs so there is no duplicates."));
            handleError(metadataRequest, errorMessage);
            throw new DuplicateUniqueNameException(errorMessage);
        }

        // Create zip
        try {
            dumpService.generateJsonZip(objectDumps, workspace);
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
    public void resetLastUpdateDate() {
        // reset last dump date if already present
        Optional<DumpConfiguration> lastDumpOpt = dumpRepository.findById(DumpConfiguration.DUMP_CONF_ID);
        if (lastDumpOpt.isPresent()) {
            DumpConfiguration lastDump = lastDumpOpt.get();
            lastDump.setLastDumpReqDate(null);
            dumpRepository.save(lastDump);
        }
    }

    @Override
    public void handleError(AIPSaveMetadataRequest metadataRequest, String errorMessage) {
        notificationClient.notify(errorMessage, String.format("Error while dumping AIPs for period %s to %s",
                                                              metadataRequest.getPreviousDumpDate(),
                                                              metadataRequest.getCreationDate()), NotificationLevel.ERROR,
                                  DefaultRole.ADMIN);
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
