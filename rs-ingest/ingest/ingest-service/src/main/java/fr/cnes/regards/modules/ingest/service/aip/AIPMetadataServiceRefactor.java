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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.dump.DumpService;
import fr.cnes.regards.framework.dump.ObjectDump;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPSaveMetadataRepositoryRefactor;
import fr.cnes.regards.modules.ingest.domain.IdsOnly;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPSaveMetadataRequestRefactor;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;

/**
 * @author Iliana Ghazali
 *
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

    private static final String TMP_DUMP_LOCATION = "target/tmpdump"; //FIXME to change

    private static final String DUMP_LOCATION = "target/dump"; //FIXME to change

    public boolean writeZips(AIPSaveMetadataRequestRefactor aipSaveMetadataRequestRefactor) {
        boolean flagError = false;

        // Get all aipIds between lastDumpDate/creationDate
        OffsetDateTime lastDumpDate = aipSaveMetadataRequestRefactor.getLastDumpDate();
        OffsetDateTime creationDate = aipSaveMetadataRequestRefactor.getCreationDate();
        Page<IdsOnly> aipToDump = aipRepository.findByLastUpdateBetween(lastDumpDate, creationDate, PageRequest
                .of(0, saveMetadataIterationLimit, Sort.by(Sort.Order.asc("creationDate"))));

        // Write Zips
        Set<String> aipIds = new HashSet<>();
        Set<AIPEntity> aipEntities;
        List<ObjectDump> objectDumps;
        List<String> duplicatedJsonNames;

        //TODO : how to handle unique json names for all pages ?
        while (aipToDump.hasNext()) {
            {
                // Retrieve all aips by id and convert to object dump
                aipToDump.getContent().forEach(id -> aipIds.add(id.getId().toString()));
                aipEntities = aipRepository.findByAipIdIn(aipIds);
                objectDumps = convertAipToObjectDump(aipEntities);

                // Check if names are unique in the collection
                duplicatedJsonNames = dumpService.checkUniqueJsonNames(objectDumps);

                // Create zip
                if (duplicatedJsonNames.isEmpty()) {
                    try {
                        // FIXME handle one page and write result on job workspace
                        dumpService.generateJsonZip(objectDumps, TMP_DUMP_LOCATION);
                    } catch (IOException e) {
                        LOGGER.error("Error during zip creation", e);
                        flagError = true;
                        handleError(aipSaveMetadataRequestRefactor);
                        break;
                    }
                    if (aipToDump.hasNext()) {
                        aipToDump.nextPageable();
                    }
                } else {
                    flagError = true;
                    handleError(aipSaveMetadataRequestRefactor);
                    break;
                }
            }
        }
        return flagError;
    }

    public void writeDump(AIPSaveMetadataRequestRefactor aipSaveMetadataRequestRefactor) {
        OffsetDateTime creationDate = aipSaveMetadataRequestRefactor.getCreationDate();
        try {
            dumpService.generateDump(DUMP_LOCATION, TMP_DUMP_LOCATION, creationDate);
            handleSuccess(aipSaveMetadataRequestRefactor);
        } catch (IOException e) {
            LOGGER.error("Error during zip creation", e);
            handleError(aipSaveMetadataRequestRefactor);
        }
    }

    //**** UTILS ****

    public List<ObjectDump> convertAipToObjectDump(Set<AIPEntity> aipEntities) {
        List objectDumps = new ArrayList();
        AIP aip;
        String jsonName;
        ObjectDump objectDump;

        for (AIPEntity aipEntity : aipEntities) {
            aip = aipEntity.getAip();
            jsonName = aipEntity.getProviderId() + "-" + aipEntity.getVersion();
            objectDump = new ObjectDump(aipEntity.getCreationDate(), jsonName, aip, aipEntity.getAipId());
            objectDumps.add(objectDump);
        }
        return objectDumps;
    }

    public void handleError(AIPSaveMetadataRequestRefactor aipSaveMetadataRequestRefactor) {
        aipSaveMetadataRequestRefactor.setState(InternalRequestState.ERROR);
        aipSaveMetadataRepositoryRefactor.save(aipSaveMetadataRequestRefactor);
        //TODO : clean workspace

    }

    public void handleSuccess(AIPSaveMetadataRequestRefactor aipSaveMetadataRequestRefactor) {
        aipSaveMetadataRepositoryRefactor.delete(aipSaveMetadataRequestRefactor);
        //TODO : clean workspace
    }

}
