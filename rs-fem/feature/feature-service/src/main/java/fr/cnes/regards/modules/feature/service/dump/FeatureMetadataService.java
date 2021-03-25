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

package fr.cnes.regards.modules.feature.service.dump;

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
import fr.cnes.regards.modules.feature.dao.IFeatureEntityRepository;
import fr.cnes.regards.modules.feature.dao.IFeatureSaveMetadataRequestRepository;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.exception.DuplicateUniqueNameException;
import fr.cnes.regards.modules.feature.domain.exception.NothingToDoException;
import fr.cnes.regards.modules.feature.domain.request.FeatureSaveMetadataRequest;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;

/**
 * see {@link IFeatureMetadataService}
 * @author Iliana Ghazali
 */

@Service
@MultitenantTransactional
public class FeatureMetadataService implements IFeatureMetadataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureMetadataService.class);

    // Limit number of features to retrieve in one page
    @Value("${regards.feature.dump.zip-limit:1000}")
    private int zipLimit;

    @Autowired
    private IFeatureSaveMetadataRequestRepository featureSaveMetadataRepository;

    @Autowired
    private IFeatureEntityRepository featureRepository;

    @Autowired
    private DumpService dumpService;

    @Autowired
    private IFeatureMetadataService self;

    @Autowired
    private INotificationClient notificationClient;

    @Override
    public void writeDump(FeatureSaveMetadataRequest metadataRequest, Path dumpLocation, Path tmpZipLocation)
            throws IOException {
        // Write dump (create a zip of multiple zips)
        dumpService.generateDump(dumpLocation, tmpZipLocation, metadataRequest.getRequestDate());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void writeZips(FeatureSaveMetadataRequest metadataRequest, Path tmpZipLocation)
            throws NothingToDoException, IOException {
        Pageable pageToRequest = PageRequest.of(0, zipLimit, Sort.by(Sort.Order.asc("creationDate")));
        try {
            do {
                // zip json files generated from features
                pageToRequest = self.dumpOnePage(metadataRequest, pageToRequest, tmpZipLocation);
            } while (pageToRequest != null);
        } catch (DuplicateUniqueNameException e) {
            self.handleError(metadataRequest, e.getMessage());
            throw new RsRuntimeException(e);
        }
    }

    @Override
    public Pageable dumpOnePage(FeatureSaveMetadataRequest metadataRequest, Pageable pageToRequest, Path tmpZipLocation)
            throws IOException, DuplicateUniqueNameException, NothingToDoException {
        // Find features to zip
        Page<FeatureEntity> featureToDump;
        OffsetDateTime previousDumpDate = metadataRequest.getPreviousDumpDate();
        OffsetDateTime dumpDate = metadataRequest.getRequestDate();
        // If previousDumpDate is null, find all features with lastUpdate < previousDumpDate
        if (previousDumpDate == null) {
            featureToDump = featureRepository.findByLastUpdateLessThan(dumpDate, pageToRequest);
        } else {
            // else find all features with previousDumpDate < lastUpdate < dumpDate
            featureToDump = featureRepository.findByLastUpdateBetween(previousDumpDate, dumpDate, pageToRequest);
        }

        // If some features were found, convert them into ObjectDump
        List<ObjectDump> objectDumps = convertFeatureToObjectDump(featureToDump.getContent());

        // If no feature was found, throw NothingToDoException
        if (objectDumps.isEmpty()) {
            throw new NothingToDoException(
                    String.format("There is nothing to dump between %s and %s", previousDumpDate, dumpDate));
        }

        // Check if json names <id_feature>-<version> are unique in the collection
        // If yes, throw DuplicateUniqueNameException
        List<ObjectDump> duplicatedJsonNames;
        duplicatedJsonNames = dumpService.checkUniqueJsonNames(objectDumps);
        if (!duplicatedJsonNames.isEmpty()) {
            String errorMessage = duplicatedJsonNames.stream().map(ObjectDump::getJsonName)
                    .collect(Collectors.joining(", ",
                                                "Some features to dump had the same generated names "
                                                        + "(providerId-version.json) should be unique: ",
                                                ". Please edit your features so there is no duplicates."));
            handleError(metadataRequest, errorMessage);
            throw new DuplicateUniqueNameException(errorMessage);
        }

        // If no error was detected, create zip that contains json files generated from features
        try {
            dumpService.generateJsonZip(objectDumps, tmpZipLocation);
        } catch (IOException e) {
            LOGGER.error("Error while dumping one page of feature", e);
            throw e;
        }
        return featureToDump.hasNext() ? featureToDump.nextPageable() : null;
    }

    private List<ObjectDump> convertFeatureToObjectDump(Collection<FeatureEntity> featureEntities) {
        return featureEntities.stream()
                .map(featureEntity -> new ObjectDump(featureEntity.getCreationDate(),
                        featureEntity.getProviderId() + "-" + featureEntity.getVersion(), featureEntity.getFeature(),
                        featureEntity.getId().toString()))
                .collect(Collectors.toList());
    }

    @Override
    public void handleError(FeatureSaveMetadataRequest metadataRequest, String errorMessage) {
        notificationClient
                .notify(errorMessage,
                        String.format("Error while dumping features for period %s to %s",
                                      metadataRequest.getPreviousDumpDate(), metadataRequest.getRequestDate()),
                        NotificationLevel.ERROR, DefaultRole.ADMIN);
        metadataRequest.addError(errorMessage);
        metadataRequest.setState(RequestState.ERROR);
        featureSaveMetadataRepository.save(metadataRequest);
        // no need to clean up workspace as job service is doing so
    }

    @Override
    public void handleSuccess(FeatureSaveMetadataRequest metadataRequest) {
        featureSaveMetadataRepository.delete(metadataRequest);
        // we do not need to clean up workspace as job service is doing so for us
    }

    @Override
    public Page<FeatureSaveMetadataRequest> findRequests(Pageable page) {
        return featureSaveMetadataRepository.findAll(page);
    }
}
