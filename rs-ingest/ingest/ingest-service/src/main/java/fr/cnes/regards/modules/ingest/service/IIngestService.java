/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service;

import java.io.InputStream;
import java.util.Collection;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.dto.RequestInfoDto;
import fr.cnes.regards.modules.ingest.domain.dto.flow.DeletionRequestFlowItem;
import fr.cnes.regards.modules.ingest.domain.dto.flow.IngestRequestFlowItem;

/**
 * Ingest service interface
 *
 * @author Marc Sordi
 *
 */
public interface IIngestService {

    /**
     * Register ingest requests from flow items
     * @param item flow items to register as ingest requests
     */
    void registerIngestRequests(Collection<IngestRequestFlowItem> items);

    /**
     * Redirect collection of SIP to data flow (REST to messages)
     * @param sips raw {@link SIPCollection}
     */
    RequestInfoDto redirectToDataflow(SIPCollection sips);

    /**
     * Redirect collection of SIP to data flow (REST to messages)
     * @param input JSON file containing a SIP collection
     */
    RequestInfoDto redirectToDataflow(InputStream input) throws ModuleException;

    /**
     * Register deletion requests from flow items
     * @param item flow items to register as deletion requests
     */
    void registerDeletionRequests(Collection<DeletionRequestFlowItem> items);

    /**
     * Delete SIPs by provider id redirected to data flow
     */
    RequestInfoDto deleteByProviderId(String providerId);

    /**
     * Delete SIPs by sip id redirected to data flow
     */
    RequestInfoDto deleteBySipId(String sipId);

    // FIXME
    //    /**
    //     * Retry to store a SIP already submitted previously.
    //     * @param sipId {@link String} ipId of the SIP to retry
    //     * @return SIP DTO
    //     * @throws ModuleException
    //     */
    //    SIPDto retryIngest(UniformResourceName sipId) throws ModuleException;
    //
    //    /**
    //     * Check if the SIP with the given ipId is available for new ingestion submission
    //     */
    //    Boolean isRetryable(UniformResourceName sipId) throws EntityNotFoundException;

}
