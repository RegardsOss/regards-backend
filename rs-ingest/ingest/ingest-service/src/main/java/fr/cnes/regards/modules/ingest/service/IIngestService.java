/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.ingest.domain.dto.SIPDto;

/**
 * Ingest service interface
 *
 * @author Marc Sordi
 *
 */
public interface IIngestService {

    /**
     * Store SIP for further asynchronous processing
     * @param sips raw {@link SIPCollection}
     * @return SIP DTO
     * @throws ModuleException if error occurs!
     */
    Collection<SIPDto> ingest(SIPCollection sips) throws ModuleException;

    /**
     * Store SIP for further asynchronous processing
     * @param input JSON file containing a SIP collection
     * @return SIP DTO
     * @throws ModuleException if error occurs!
     */
    Collection<SIPDto> ingest(InputStream input) throws ModuleException;

    /**
     * Retry to store a SIP already submitted previously.
     * @param ipId {@link String} ipId of the SIP to retry
     * @return SIP DTO
     * @throws ModuleException
     */
    SIPDto retryIngest(String ipId) throws ModuleException;
}
