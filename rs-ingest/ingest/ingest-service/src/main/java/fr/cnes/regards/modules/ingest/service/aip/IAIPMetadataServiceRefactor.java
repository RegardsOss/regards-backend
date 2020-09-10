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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import fr.cnes.regards.framework.dump.ObjectDump;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.modules.ingest.domain.IdsOnly;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPSaveMetadataRequestRefactor;
import fr.cnes.regards.modules.ingest.domain.request.manifest.StoreLocation;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;

/**
 * Manage AIP storage
 * @author Iliana Ghazali
 */
public interface IAIPMetadataServiceRefactor {

    boolean writeZips(AIPSaveMetadataRequestRefactor aipSaveMetadataRequestRefactor);

    void writeDump(AIPSaveMetadataRequestRefactor aipSaveMetadataRequestRefactor);

    //**** UTILS ****

    List<ObjectDump> convertAipToObjectDump(Set<AIPEntity> aipEntities);

    void handleError(AIPSaveMetadataRequestRefactor aipSaveMetadataRequestRefactor);

    void handleSuccess(AIPSaveMetadataRequestRefactor aipSaveMetadataRequestRefactor);

}
