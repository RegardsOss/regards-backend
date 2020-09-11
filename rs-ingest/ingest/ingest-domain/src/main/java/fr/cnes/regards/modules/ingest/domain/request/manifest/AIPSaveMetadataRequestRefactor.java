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
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;

/**
 * Storing info that a metadata should be saved on storage
 * @author Iliana Ghazali
 */
@Entity(name = RequestTypeConstant.STORE_METADATA_VALUE)
public class AIPSaveMetadataRequestRefactor extends AbstractRequest {

    @NotNull(message = "Last dump date")
    @Column(name = "last_dump_date")
    private OffsetDateTime lastDumpDate;

    public AIPSaveMetadataRequestRefactor(OffsetDateTime lastDumpDate) {
        // session information are specific to AIP subset defined by users, the same goes for session owner. ProviderId is aip specific.
        // AIPSaveMetadataRequests are not related to sessions but subset AIPs only by date
        super(null, null, null);
        this.lastDumpDate = lastDumpDate;
    }

    public OffsetDateTime getLastDumpDate() {
        return lastDumpDate;
    }

    public void setLastDumpDate(OffsetDateTime lastDumpDate) {
        this.lastDumpDate = lastDumpDate;
    }
}
