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
package fr.cnes.regards.modules.ingest.domain.request.dump;

import javax.persistence.*;
import java.time.OffsetDateTime;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;

/**
 * Storing info that a metadata should be saved on storage
 * @author Iliana Ghazali
 */
@Entity
@DiscriminatorValue(RequestTypeConstant.AIP_SAVE_METADATA_VALUE)
public class AIPSaveMetadataRequestRefactor extends AbstractRequest {

    @Column(name = "previous_dump_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime previousDumpDate;

    public AIPSaveMetadataRequestRefactor(OffsetDateTime previousDumpDate) {
        // session information are specific to AIP subset defined by users, the same goes for session owner. ProviderId is aip specific.
        // AIPSaveMetadataRequests are not related to sessions but subset AIPs only by date
        super(null, null, null, RequestTypeConstant.AIP_SAVE_METADATA_VALUE);
        this.previousDumpDate = previousDumpDate;
    }

    public AIPSaveMetadataRequestRefactor() {
    }

    public OffsetDateTime getPreviousDumpDate() {
        return previousDumpDate;
    }

    public void setPreviousDumpDate(OffsetDateTime previousDumpDate) {
        this.previousDumpDate = previousDumpDate;
    }
}
