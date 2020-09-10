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
package fr.cnes.regards.modules.ingest.domain.request.manifest;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;

/**
 * Storing info that a metadata should be saved on storage
 * @author Iliana Ghazali
 */
@Entity(name = RequestTypeConstant.STORE_METADATA_VALUE)
public class AIPSaveMetadataRequestRefactor extends AbstractRequest {

    @NotNull(message = "Last dump date")
    @Column(name = "last_dump_date", nullable = false)
    private OffsetDateTime lastDumpDate;

    public OffsetDateTime getLastDumpDate() {
        return lastDumpDate;
    }

    public void setLastDumpDate(OffsetDateTime lastDumpDate) {
        this.lastDumpDate = lastDumpDate;
    }

    public static AIPSaveMetadataRequestRefactor build(OffsetDateTime lastDumpDate) {
        AIPSaveMetadataRequestRefactor smdr = new AIPSaveMetadataRequestRefactor();
        smdr.lastDumpDate = lastDumpDate;
        smdr.setState(InternalRequestState.TO_SCHEDULE);
        smdr.setDtype(RequestTypeConstant.STORE_METADATA_VALUE);
        smdr.setCreationDate(OffsetDateTime.now());
        return smdr;
    }
}
