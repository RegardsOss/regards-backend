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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import fr.cnes.regards.framework.geojson.AbstractFeature;
import fr.cnes.regards.framework.oais.urn.EntityType;

/**
 *
 * OAIS Information package base structure
 *
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 */
public abstract class AbstractInformationPackage<ID> extends AbstractFeature<InformationPackageProperties, ID> {

    @NotNull
    private EntityType ipType;

    public Collection<String> getTags() {
        return properties.getPdi().getTags();
    }

    public List<Event> getHistory() {
        return properties.getPdi().getProvenanceInformation().getHistory();
    }

    /**
     * Abstraction on where the last event is and how to get it
     * @return last event occurred to this aip
     */
    public Event getLastEvent() {
        List<Event> history = getHistory();
        if (!history.isEmpty()) {
            return history.get(history.size() - 1);
        } else {
            return null;
        }
    }

    public Event getSubmissionEvent() {
        return getHistory().stream().filter(e -> EventType.SUBMISSION.name().equals(e.getType())).findFirst()
                .orElse(null);
    }

    public void addEvent(@Nullable String type, String comment, OffsetDateTime date) {
        Event event = new Event();
        event.setType(type);
        event.setComment(comment);
        event.setDate(date);
        getHistory().add(event);
    }

    public void addEvent(@Nullable String type, String comment) {
        addEvent(type, comment, OffsetDateTime.now());
    }

    public EntityType getIpType() {
        return ipType;
    }

    public void setIpType(EntityType ipType) {
        this.ipType = ipType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = (prime * result) + ((ipType == null) ? 0 : ipType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        AbstractInformationPackage other = (AbstractInformationPackage) obj;
        if (ipType != other.ipType) {
            return false;
        }
        return true;
    }

}
