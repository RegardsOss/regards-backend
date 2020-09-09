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
package fr.cnes.regards.modules.ingest.service.request;

import java.util.Collection;
import java.util.List;

import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPSaveMetadataRequestRefactor;
import fr.cnes.regards.modules.storage.client.RequestInfo;

/**
 * Service to handle {@link AIPStoreMetaDataRequest}s
 *
 * @author Léo Mieulet
 */
public interface IAIPSaveMetadataRequestServiceRefactor {


    /**
     * Schedule new {@link AIPStoreMetaDataRequest}s associated to given {@link AIPEntity}s
     */
    void createRequest();

    /**
     * @param ids a list of request id
     * @return the list of entities
     */
    List<AIPSaveMetadataRequestRefactor> search(List<Long> ids);

    /**
     * Callback when a {@link AIPStoreMetaDataRequest} is terminated successfully.
     * @param request {@link AIPStoreMetaDataRequest}
     */
    void handleSuccesses(Collection<AIPSaveMetadataRequestRefactor> request);

    /**
     * Callback when a  {@link AIPStoreMetaDataRequest} is terminated with errors.
     * @param request {@link AIPStoreMetaDataRequest}
     * @param requestInfo {@link RequestInfo}
     */
    void handleError(AIPSaveMetadataRequestRefactor request, RequestInfo requestInfo);
}
