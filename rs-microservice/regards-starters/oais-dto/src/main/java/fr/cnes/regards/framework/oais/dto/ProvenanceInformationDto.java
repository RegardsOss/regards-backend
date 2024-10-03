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
package fr.cnes.regards.framework.oais.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.lang.Nullable;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OAIS Preservation Description Information object
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 * @author Michael Nguyen
 */
public class ProvenanceInformationDto {

    public static final String ORIGIN_URN = "originUrn";

    /**
     * The history
     */
    @Schema(description = "Package history information.")
    private final List<EventDto> history = new ArrayList<>();

    /**
     * The facility
     */
    private String facility;

    /**
     * The instrument
     */
    private String instrument;

    /**
     * The filter
     */
    private String filter;

    /**
     * The detector
     */
    private String detector;

    /**
     * The proposal
     */
    private String proposal;

    private Map<String, Object> additional;

    public String getFacility() {
        return facility;
    }

    public void setFacility(String facility) {
        this.facility = facility;
    }

    public List<EventDto> getHistory() {
        return history;
    }

    public Map<String, Object> getAdditional() {
        if (additional == null) {
            additional = new HashMap<>();
        }
        return additional;
    }

    /**
     * @return the instrument
     */
    public String getInstrument() {
        return instrument;
    }

    /**
     * Set the instrument
     */
    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    /**
     * @return the filter
     */
    public String getFilter() {
        return filter;
    }

    /**
     * Set the filter
     */
    public void setFilter(String filter) {
        this.filter = filter;
    }

    /**
     * @return the detector
     */
    public String getDetector() {
        return detector;
    }

    /**
     * Set the detector
     */
    public void setDetector(String detector) {
        this.detector = detector;
    }

    /**
     * @return the proposal
     */
    public String getProposal() {
        return proposal;
    }

    /**
     * Set the proposal
     */
    public void setProposal(String proposal) {
        this.proposal = proposal;
    }

    public void setAdditionalOriginUrn(String originUrn) {
        getAdditional().put(ORIGIN_URN, originUrn);
    }

    public void addEvent(@Nullable String type, String comment, OffsetDateTime date) {
        EventDto event = new EventDto();
        event.setType(type);
        event.setComment(comment);
        event.setDate(date);
        List<EventDto> actualHistory = getHistory();
        if (!actualHistory.contains(event)) {
            actualHistory.add(event);
        }
    }

    public void addEvent(@Nullable String type, String comment) {
        addEvent(type, comment, OffsetDateTime.now());
    }

    @Schema(description = "URN of the origin package. Used to link the package with an existing package on an other "
                          + "catalog.")
    public String getOriginUrn() {
        return (String) getAdditional().get(ORIGIN_URN);
    }

    public void setOriginUrn(String originUrn) {
        getAdditional().put(ORIGIN_URN, originUrn);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (additional == null ? 0 : additional.hashCode());
        result = prime * result + (facility == null ? 0 : facility.hashCode());
        result = prime * result + history.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ProvenanceInformationDto other = (ProvenanceInformationDto) obj;
        if (additional == null) {
            if (other.additional != null) {
                return false;
            }
        } else if (!additional.equals(other.additional)) {
            return false;
        }
        if (facility == null) {
            if (other.facility != null) {
                return false;
            }
        } else if (!facility.equals(other.facility)) {
            return false;
        }
        return history.containsAll(other.history);
    }

}
