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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.ingest.dto;

import fr.cnes.regards.framework.oais.dto.sip.SIPDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.UUID;

/**
 * @author mnguyen0
 */
public class IngestRequestFlowItemDto {

    private SIPDto sip;

    @Valid
    @NotNull(message = IngestRequestMessages.MISSING_METADATA)
    private IngestMetadataDto metadata;

    protected String requestId;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setSip(SIPDto sip) {
        this.sip = sip;
    }

    public void setMetadata(IngestMetadataDto metadata) {
        this.metadata = metadata;
    }

    public SIPDto getSip() {
        return sip;
    }

    public IngestMetadataDto getMetadata() {
        return metadata;
    }

    public static String generateRequestId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public String toString() {
        return "IngestRequestFlowItemDto{"
               + "sip="
               + sip
               + ", metadata="
               + metadata
               + ", requestId='"
               + requestId
               + '\''
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IngestRequestFlowItemDto that = (IngestRequestFlowItemDto) o;
        return Objects.equals(sip, that.sip) && Objects.equals(metadata, that.metadata) && Objects.equals(requestId,
                                                                                                          that.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sip, metadata, requestId);
    }
}
