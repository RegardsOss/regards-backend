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
import fr.cnes.regards.modules.ingest.domain.dto.RequestInfoDto;
import fr.cnes.regards.modules.ingest.domain.request.IngestRequest;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionRequestDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;

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
    Collection<IngestRequest> registerIngestRequests(Collection<IngestRequestFlowItem> items);

    /**
     * Register and schedule ingest requests from flow items
     * @param item flow items to register as ingest requests and to schedule as an ingestion job
     */
    Collection<IngestRequest> registerAndScheduleIngestRequests(Collection<IngestRequestFlowItem> items);

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
     * Register deletion request from flow item
     * @param item flow item to register as deletion request
     */
    SessionDeletionRequestDto registerSessionDeletionRequest(SessionDeletionRequestDto request);
}
