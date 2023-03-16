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
package fr.cnes.regards.modules.ingest.domain.request.dump;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.OffsetDateTime;

/**
 * Request to save aip metadata
 *
 * @author Iliana Ghazali
 */
@Entity
@DiscriminatorValue(RequestTypeConstant.AIP_SAVE_METADATA_VALUE)
public class AIPSaveMetadataRequest extends AbstractRequest {

    @Column(name = "previous_dump_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime previousDumpDate;

    @Column(name = "dump_location")
    private String dumpLocation;

    public AIPSaveMetadataRequest(OffsetDateTime previousDumpDate, String dumpLocation) {
        // session information are specific to AIP subset defined by users, the same goes for session owner. ProviderId is aip specific.
        // AIPSaveMetadataRequests are not related to sessions
        super(null, null, null, null, RequestTypeConstant.AIP_SAVE_METADATA_VALUE);
        this.previousDumpDate = previousDumpDate;
        this.dumpLocation = dumpLocation;
    }

    public AIPSaveMetadataRequest() {
    }

    public OffsetDateTime getPreviousDumpDate() {
        return previousDumpDate;
    }

    public void setPreviousDumpDate(OffsetDateTime previousDumpDate) {
        this.previousDumpDate = previousDumpDate;
    }

    public String getDumpLocation() {
        return dumpLocation;
    }

    public void setDumpLocation(String dumpLocation) {
        this.dumpLocation = dumpLocation;
    }
}
