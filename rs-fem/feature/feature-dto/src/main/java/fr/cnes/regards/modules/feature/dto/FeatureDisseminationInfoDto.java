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
package fr.cnes.regards.modules.feature.dto;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * @author Léo Mieulet
 */
public class FeatureDisseminationInfoDto {

    private final String label;

    private final OffsetDateTime requestDate;

    private final OffsetDateTime ackDate;

    private final boolean blocking;

    public FeatureDisseminationInfoDto(String label,
                                       OffsetDateTime requestDate,
                                       OffsetDateTime ackDate,
                                       boolean blocking) {
        this.label = label;
        this.requestDate = requestDate;
        this.ackDate = ackDate;
        this.blocking = blocking;
    }

    public String getLabel() {
        return label;
    }

    public OffsetDateTime getRequestDate() {
        return requestDate;
    }

    public OffsetDateTime getAckDate() {
        return ackDate;
    }

    public boolean isBlocking() {
        return blocking;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FeatureDisseminationInfoDto that = (FeatureDisseminationInfoDto) o;
        return blocking == that.blocking && Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, blocking);
    }
}
