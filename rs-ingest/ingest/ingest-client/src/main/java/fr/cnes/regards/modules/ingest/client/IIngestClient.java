/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.client;

import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.modules.ingest.dto.IngestMetadataDto;

/**
 * Client interface for requesting the ingest service
 * <p>
 * Client requests are done asynchronously.
 * To listen to the feedback messages, you have to implement your own {@link IIngestClientListener}.
 */
public interface IIngestClient {

    /**
     * Requests a SIP ingestion with specified ingestion metadata.
     * <br/>
     *
     * @param ingestMetadata related {@link IngestMetadataDto}
     * @param sip            the {@link SIPDto} to ingest
     * @return {@link RequestInfo} containing a unique request id. This request id can
     * be used to identify responses in your {@link IIngestClientListener} implementation.
     * @throws IngestClientException if error occurs preparing ingestion submission
     */
    RequestInfo ingest(IngestMetadataDto ingestMetadata, SIPDto sip) throws IngestClientException;
}
