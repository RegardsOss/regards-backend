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
package fr.cnes.regards.modules.ingest.dto.request;

import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;

/**
 * Type of requests available through the REST endpoint
 */
public enum RequestTypeEnum {

    /**
     * Ingest requests
     */
    INGEST(RequestTypeConstant.INGEST_VALUE),
    /**
     * 1 AIP Update requests
     */
    UPDATE(RequestTypeConstant.UPDATE_VALUE),
    /**
     * A list of AIP modification + a list of criteria to find AIP
     */
    AIP_UPDATES_CREATOR(RequestTypeConstant.AIP_UPDATES_CREATOR_VALUE),

    /**
     * Dump AIP metadata
     */
    AIP_SAVE_METADATA(RequestTypeConstant.AIP_SAVE_METADATA_VALUE),

    /**
     * Postprocess AIPs
     */
    AIP_POST_PROCESS(RequestTypeConstant.AIP_POST_PROCESS_VALUE),

    /**
     * 1 OAIS (SIP and AIP) Remove request
     */
    OAIS_DELETION(RequestTypeConstant.OAIS_DELETION_VALUE),
    /**
     * A list of criteria to find AIP. It creates an OAIS_DELETION request foreach OAIS entity to remove
     */
    OAIS_DELETION_CREATOR(RequestTypeConstant.OAIS_DELETION_CREATOR_VALUE);

    RequestTypeEnum(String value) {
        if (!value.equals(this.name())) {
            throw new IllegalArgumentException("Some issue occured with " + value);
        }
        if (value.length() > AbstractRequest.MAX_TYPE_LENGTH) {
            throw new IllegalArgumentException("Enumerate value too long");
        }
    }

}
