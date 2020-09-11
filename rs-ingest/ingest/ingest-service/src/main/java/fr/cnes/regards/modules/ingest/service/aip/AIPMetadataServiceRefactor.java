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
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPSaveMetadataRepositoryRefactor;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.exception.DuplicateUniqueNameException;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPSaveMetadataRequestRefactor;

/**
 * Service to dump aips
 * @author Iliana Ghazali
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@MultitenantTransactional
public class AIPMetadataServiceRefactor implements IAIPMetadataServiceRefactor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPMetadataServiceRefactor.class);

    @Autowired
    private DumpService dumpService;

    @Autowired
    private IAIPSaveMetadataRepositoryRefactor aipSaveMetadataRepositoryRefactor;

    @Autowired
    private IAIPRepository aipRepository;

    /** Limit number of AIPs to retrieve in one page. */
    @Value("${regards.aips.save-metadata.scan.iteration-limit:1000}")
    private int saveMetadataIterationLimit;

    @Autowired
    private IAIPMetadataServiceRefactor self;

    @Autowired
    private INotificationClient notificationClient;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void writeZips(AIPSaveMetadataRequestRefactor aipSaveMetadataRequestRefactor, Path workspace) {
        Pageable pageToRequest = PageRequest.of(0, saveMetadataIterationLimit, Sort.by(Sort.Order.asc("creationDate")));
        try {
            do {
                pageToRequest = self.dumpOnePage(aipSaveMetadataRequestRefactor, pageToRequest, workspace);
            } while (pageToRequest != null);
        } catch (IOException e) {
            String errorMessage = e.getClass().getSimpleName() + " " + e.getMessage();
            self.handleError(aipSaveMetadataRequestRefactor, errorMessage);
            throw new RsRuntimeException(errorMessage, e);
        } catch (DuplicateUniqueNameException e) {
            self.handleError(aipSaveMetadataRequestRefactor, e.getMessage());
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public void writeDump(AIPSaveMetadataRequestRefactor aipSaveMetadataRequestRefactor, Path workspace) {
        OffsetDateTime creationDate = aipSaveMetadataRequestRefactor.getCreationDate();
        try {
            dumpService.generateDump(workspace, creationDate);
            handleSuccess(aipSaveMetadataRequestRefactor);
        } catch (IOException e) {
            LOGGER.error("Error while writing aip dump", e);
            throw new RsRuntimeException(e.getClass().getSimpleName() + " " + e.getMessage(), e);
        }
    }

    private List<ObjectDump> convertAipToObjectDump(Collection<AIPEntity> aipEntities) {
        return aipEntities.stream().map(aipEntity -> new ObjectDump(aipEntity.getCreationDate(),
                                                                    aipEntity.getProviderId() + "-" + aipEntity
                                                                            .getVersion(), aipEntity.getAip(),
                                                                    aipEntity.getAipId())).collect(Collectors.toList());
    }

    @Override
    public void handleError(AIPSaveMetadataRequestRefactor dumpRequest, String errorMessage) {
        notificationClient.notify(errorMessage, String.format("Error while dumping AIPs for period %s to %s",
                                                              dumpRequest.getLastDumpDate(),
                                                              dumpRequest.getCreationDate()), NotificationLevel.ERROR,
                                  DefaultRole.ADMIN);
        dumpRequest.addError(errorMessage);
        dumpRequest.setState(InternalRequestState.ERROR);
        aipSaveMetadataRepositoryRefactor.save(dumpRequest);
        // we do not need to clean up workspace as job service is doing so for us
    }

    @Override
    public void handleSuccess(AIPSaveMetadataRequestRefactor aipSaveMetadataRequestRefactor) {
        aipSaveMetadataRepositoryRefactor.delete(aipSaveMetadataRequestRefactor);
        // we do not need to clean up workspace as job service is doing so for us
    }

    @Override
    public Pageable dumpOnePage(AIPSaveMetadataRequestRefactor aipSaveMetadataRequestRefactor, Pageable pageToRequest,
            Path workspace) throws IOException, DuplicateUniqueNameException {
        // Write Zips
        List<ObjectDump> objectDumps;
        List<ObjectDump> duplicatedJsonNames;

        Page<AIPEntity> aipToDump = aipRepository
                .findByLastUpdateBetween(aipSaveMetadataRequestRefactor.getLastDumpDate(),
                                         aipSaveMetadataRequestRefactor.getCreationDate(), pageToRequest);
        objectDumps = convertAipToObjectDump(aipToDump.getContent());

        // Check if names are unique in the collection
        duplicatedJsonNames = dumpService.checkUniqueJsonNames(objectDumps);
        if (!duplicatedJsonNames.isEmpty()) {
            String errorMessage = duplicatedJsonNames.stream().map(ObjectDump::getJsonName).collect(Collectors.joining(
                    ", ", "Some AIPs to dump had same generated names "
                            + "(providerId-version.json) which should be unique: ",
                    ". Please edit your AIPs so there is no duplicates."));
            handleError(aipSaveMetadataRequestRefactor, errorMessage);
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

}
