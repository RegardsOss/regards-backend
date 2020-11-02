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


package fr.cnes.regards.modules.feature.domain.request;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.Set;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.feature.dto.PriorityLevel;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;

/**
 * Request to save feature metadata
 * @author Iliana Ghazali
 */

@Entity
@DiscriminatorValue(AbstractFeatureRequest.FEATURE_SAVE_METADATA)
public class FeatureSaveMetadataRequest extends AbstractFeatureRequest {

    @Column(name = "previous_dump_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime previousDumpDate;

    @Column(name = "dump_location")
    private String dumpLocation;

    public static FeatureSaveMetadataRequest build(String requestId, String requestOwner, OffsetDateTime requestDate,
            RequestState state, Set<String> errors, FeatureRequestStep step, PriorityLevel priority,
            OffsetDateTime previousDumpDate, String dumpLocation) {
        FeatureSaveMetadataRequest request = new FeatureSaveMetadataRequest();
        request.with(requestId, requestOwner, requestDate, state, step, priority, errors);
        request.setPreviousDumpDate(previousDumpDate);
        request.setDumpLocation(dumpLocation);
        return request;
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

    @Override
    public <U> U accept(IAbstractFeatureRequestVisitor<U> visitor) {
        return null;
    }
}
