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
package fr.cnes.regards.modules.ingest.service;

import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.domain.dto.RequestInfoDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;

import java.io.InputStream;
import java.util.Collection;

/**
 * Ingest service interface
 *
 * @author Marc Sordi
 */
public interface IIngestService {

    /**
     * Register and schedule ingest requests from flow items
     *
     * @param items flow items to register as ingest requests and to schedule as an ingestion job
     */
    void handleIngestRequests(Collection<IngestRequestFlowItem> items);

    /**
     * Handle SIP collection directly scheduling a generation job
     *
     * @param sips raw {@link SIPCollection}
     * @throws EntityInvalidException if max bulk size exceeded
     */
    RequestInfoDto handleSIPCollection(SIPCollection sips) throws EntityInvalidException;

    /**
     * Handle SIP collection directly scheduling a generation job
     *
     * @param input JSON file containing a SIP collection
     */
    RequestInfoDto handleSIPCollection(InputStream input) throws ModuleException;

}
