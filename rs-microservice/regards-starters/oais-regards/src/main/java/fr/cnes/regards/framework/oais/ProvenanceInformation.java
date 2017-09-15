/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.oais;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

/**
 * OAIS Preservation Description Information object
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 *
 */
public class ProvenanceInformation {

    @NotNull
    private String facility;

    @NotNull
    private List<Event> history = new ArrayList<>();

    private Map<String, Object> additional;

    public String getFacility() {
        return facility;
    }

    public void setFacility(String facility) {
        this.facility = facility;
    }

    public List<Event> getHistory() {
        return history;
    }

    public Map<String, Object> getAdditional() {
        if (additional == null) {
            additional = new HashMap<>();
        }
        return additional;
    }

    // TODO remove
    @Deprecated
    public ProvenanceInformation generate() {
        facility = "TestPerf";
        history = new ArrayList<>();
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((additional == null) ? 0 : additional.hashCode());
        result = (prime * result) + ((facility == null) ? 0 : facility.hashCode());
        result = (prime * result) + ((history == null) ? 0 : history.hashCode());
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
        ProvenanceInformation other = (ProvenanceInformation) obj;
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
        if (history == null) {
            if (other.history != null) {
                return false;
            }
        } else if (!history.containsAll(other.history)) {
            return false;
        }
        return true;
    }

}
