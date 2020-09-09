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
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.dump.DumpService;
import fr.cnes.regards.framework.dump.ObjectDump;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.dao.IAIPDumpMetadataRepositoryRefactor;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPSaveMetadataRepositoryRefactor;
import fr.cnes.regards.modules.ingest.domain.IdsOnly;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPSaveMetadataRequestRefactor;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.service.request.IAIPSaveMetadataRequestServiceRefactor;

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
    private IAIPSaveMetadataRequestServiceRefactor aipSaveMetadataRequestServiceRefactor;

    @Autowired
    private IAIPSaveMetadataRepositoryRefactor aipStoreMetaDataRepositoryRefactor;

    @Autowired
    private IAIPDumpMetadataRepositoryRefactor aipDumpMetadataRepositoryRefactor;

    @Autowired
    private IAIPRepository aipRepository;

    /**
     * Limit number of AIPs to retrieve in one page.!!!!! Must be superior to regards.json.dump.max.per.sub.zip
     */
    @Value("${regards.aips.save-metadata.scan.iteration-limit:1000}")
    private int saveMetadataIterationLimit;

    public void dumpJson(AIPSaveMetadataRequestRefactor aipSaveMetadataRequest) throws Exception {
        //get last dump date
        OffsetDateTime lastDumpDate = aipDumpMetadataRepositoryRefactor.findLastDumpDate();
        //get creation date of the request
        OffsetDateTime dateRequest = aipSaveMetadataRequest.getCreationDate();
        //get all aipIds between these dates
        Page<IdsOnly> aipToDump = aipRepository
                .findByLastUpdateBetweenOrderByCreationDateAsc(lastDumpDate, dateRequest, PageRequest.of(0, saveMetadataIterationLimit));

        Set<String> aipIds = new HashSet<>();
        Set<AIPEntity> aipEntities;
        List<ObjectDump> objectDumps;
        List<String> duplicatedJsonNames;

        while (aipToDump.hasNext()) {
            //get aipIds
            aipToDump.getContent().forEach(id -> aipIds.add(id.getId().toString()));
            //retrieve all aips
            aipEntities = aipRepository.findByAipIdIn(aipIds);
            objectDumps = convertAipToObjectDump(aipEntities);
            //check if names are unique in the collection
            duplicatedJsonNames = dumpService.checkUniqueJsonNames(objectDumps);
            //create zips
            if (duplicatedJsonNames.isEmpty()) {
                try {
                    dumpService.generateJsonZips(objectDumps, "target/dump");
                } catch (IOException e) {
                    LOGGER.error("Error during zip creation",e);
                }

                if (aipToDump.hasNext()) {
                    aipToDump.nextPageable();
                }
            } else {
                //TODO ?
            }
        }
    }

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

}
