/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Collection;
import java.util.Set;

/**
 * Callback listener for ingestion request : {@link IIngestClient#ingest(fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto, fr.cnes.regards.modules.ingest.dto.sip.SIP)}
 *
 * @author Marc SORDI
 */
public interface IIngestClientListener {

    /**
     * Callback on request denied
     *
     * @param infos {@link RequestInfo}s to track request
     */
    void onDenied(Collection<RequestInfo> infos);

    /**
     * Callback on request granted
     *
     * @param infos {@link RequestInfo}s to track request
     */
    void onGranted(Collection<RequestInfo> infos);

    /**
     * Callback on request error
     *
     * @param infos {@link RequestInfo}s to track request
     */
    void onError(Collection<RequestInfo> infos);

    /**
     * Callback on request success
     *
     * @param infos {@link RequestInfo}s to track request
     */
    void onSuccess(Collection<RequestInfo> infos);

    /**
     * Callback on request deleted
     */
    void onDeleted(Set<RequestInfo> deleted);
}
