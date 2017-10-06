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

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.Sets;
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

    public EntityType getIpType() {
        return properties.getIpType();
    }

    public void setIpType(EntityType entityType) {
        properties.setIpType(entityType);
    }

    public Collection<String> getTags() {
        if (properties.getPdi().getContextInformation()
                .containsKey(PreservationDescriptionInformation.CONTEXT_INFO_TAGS_KEY)) {
            return (Collection<String>) properties.getPdi().getContextInformation()
                    .get(PreservationDescriptionInformation.CONTEXT_INFO_TAGS_KEY);
        } else {
            return Sets.newHashSet();
        }
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
        if (history.size() != 0) {
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

}
