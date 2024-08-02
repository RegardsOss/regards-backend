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
package fr.cnes.regards.modules.ingest.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.beans.ConstructorProperties;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * This class contains information about the dissemination of an AIP from a Regards instance to another one
 *
 * @author mnguyen0
 **/
public class DisseminationInfoDto {

    /**
     * Name of the Regards destination
     */
    private final String label;

    /**
     * Date of the dissemination request
     */
    private final OffsetDateTime date;

    /**
     * Date of acknowledge of the dissemination request
     */
    private OffsetDateTime ackDate;

    @ConstructorProperties({ "label", "date" })
    @JsonCreator
    public DisseminationInfoDto(@JsonProperty("label") String label, @JsonProperty("date") OffsetDateTime date) {
        this.label = label;
        this.date = date;
    }

    public DisseminationInfoDto(String label, OffsetDateTime date, OffsetDateTime ackDate) {
        this(label, date);
        this.ackDate = ackDate;
    }

    public boolean hasReceivedAck() {
        return ackDate != null;
    }

    public boolean hasInitialDate() {
        return date != null;
    }

    public void setAckDate(OffsetDateTime ackDate) {
        this.ackDate = ackDate;
    }

    public String getLabel() {
        return label;
    }

    public OffsetDateTime getDate() {
        return date;
    }

    public OffsetDateTime getAckDate() {
        return ackDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DisseminationInfoDto that = (DisseminationInfoDto) o;
        return Objects.equals(label, that.label) && Objects.equals(date, that.date) && Objects.equals(ackDate,
                                                                                                      that.ackDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, date, ackDate);
    }

    @Override
    public String toString() {
        return "DisseminationInfo{" + "label='" + label + '\'' + ", date=" + date + ", ackDate=" + ackDate + '}';
    }
}