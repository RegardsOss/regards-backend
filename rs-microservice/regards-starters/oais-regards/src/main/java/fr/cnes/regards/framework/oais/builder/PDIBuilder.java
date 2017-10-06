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
package fr.cnes.regards.framework.oais.builder;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.springframework.util.Assert;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.PreservationDescriptionInformation;

/**
 *
 * Preservation Description Information Builder
 * @author Marc Sordi
 *
 */
public class PDIBuilder implements IOAISBuilder<PreservationDescriptionInformation> {

    private final PreservationDescriptionInformation pdi = new PreservationDescriptionInformation();

    @Override
    public PreservationDescriptionInformation build() {
        return pdi;
    }

    /**
     * Add tags
     * @param tags tags to add
     */
    public void addTags(String... tags) {
        Assert.notEmpty(tags, "Tag is required");
        Collection<String> existingTags = (Collection<String>) pdi.getContextInformation()
                .get(PreservationDescriptionInformation.CONTEXT_INFO_TAGS_KEY);
        if (existingTags == null) {
            existingTags = Sets.newHashSet(tags);
        } else {
            existingTags.addAll(Arrays.asList(tags));
        }
    }

    /**
     * Add an <b>optional</b> context information
     * @param key information key
     * @param value information
     */
    public void addContextInformation(String key, Object value) {
        Assert.hasLength(key, "Context information key is required");
        Assert.isTrue(!key.equalsIgnoreCase(PreservationDescriptionInformation.CONTEXT_INFO_TAGS_KEY),
                      "Tags must be added thanks to addTags method");
        Assert.notNull(value, "Context information value is required");
        pdi.getContextInformation().put(key, value);
    }

    /**
     * Add optional context information
     * @param key information key
     * @param value information
     */
    public void addReferenceInformation(String key, String value) {
        Assert.hasLength(key, "Reference information key is required");
        Assert.notNull(value, "Reference information value is required");
        pdi.getReferenceInformation().put(key, value);
    }

    /**
     * Set <b>required</b> provenance information
     * @param facility required facility
     * @param additional optional additional information (may be null)
     */
    public void setProvenanceInformation(String facility, @Nullable Map<String, Object> additional) {
        Assert.hasLength(facility, "Facility is required");
        pdi.getProvenanceInformation().setFacility(facility);

        if (additional != null) {
            pdi.getProvenanceInformation().getAdditional().putAll(additional);
        }
    }

    /**
     * add additional provenance information
     * @param key name of the information
     * @param value value of the information
     */
    public void addProvenanceInformation(String key, Object value) {
        Assert.hasLength(key, "Name of the additional provenance information is required");
        Assert.notNull(value, "Value of the additional provenance information is required");
        pdi.getProvenanceInformation().getAdditional().put(key, value);
    }

    /**
     * Set optional facility information
     */
    public void setFacility(String facility) {
        Assert.hasLength(facility, "Facility cannot be empty");
        pdi.getProvenanceInformation().setFacility(facility);
    }

    /**
     * Set optional instrument information
     */
    public void setInstrument(String instrument) {
        Assert.hasLength(instrument, "Instrument cannot be empty");
        pdi.getProvenanceInformation().setInstrument(instrument);
    }

    /**
     * Set optional filter information
     */
    public void setFilter(String filter) {
        Assert.hasLength(filter, "Filter cannot be empty");
        pdi.getProvenanceInformation().setFilter(filter);
    }

    /**
     * Set optional detector information
     */
    public void setDetector(String detector) {
        Assert.hasLength(detector, "Detector cannot be empty");
        pdi.getProvenanceInformation().setDetector(detector);
    }

    /**
     * Set optional proposal information
     */
    public void setProposal(String proposal) {
        Assert.hasLength(proposal, "Proposal cannot be empty");
        pdi.getProvenanceInformation().setProposal(proposal);
    }

    /**
     * Alias for {@link PDIBuilder#setProvenanceInformation(String, Map)} (no additional)
     * @param facility required facility
     */
    public void setProvenanceInformation(String facility) {
        setProvenanceInformation(facility, null);
    }

    /**
     * Add information object events
     * @param events events to add
     */
    public void addProvenanceInformationEvents(Event... events) {
        Assert.notEmpty(events, "At least one event is required if this method is called");
        for (Event event : events) {
            Assert.hasLength(event.getComment(), "Event comment is required");
            Assert.notNull(event.getDate(), "Event date is required");
            pdi.getProvenanceInformation().getHistory().add(event);
        }
    }

    /**
     * Add an information object event
     * @param type optional event type key (may be null)
     * @param comment event comment
     * @param date event date
     */
    public void addProvenanceInformationEvent(@Nullable String type, String comment, OffsetDateTime date) {
        Event event = new Event();
        event.setType(type);
        event.setComment(comment);
        event.setDate(date);
        addProvenanceInformationEvents(event);
    }

    /**
     * Add an information object event
     * @param comment event comment
     * @param date event date
     */
    public void addProvenanceInformationEvent(String comment, OffsetDateTime date) {
        addProvenanceInformationEvent(null, comment, date);
    }

    /**
     * Add an information object event
     * @param comment event comment
     */
    public void addProvenanceInformationEvent(String comment) {
        addProvenanceInformationEvent(null, comment, OffsetDateTime.now());
    }

    /**
     * Set <b>required</b> access right information
     * @param licence optional licence
     * @param dataRights secure key
     * @param publicReleaseDate optional public release date (may be null)
     */
    public void setAccessRightInformation(String licence, String dataRights,
            @Nullable OffsetDateTime publicReleaseDate) {
        Assert.hasLength(dataRights, "Data rights is required");
        pdi.getAccesRightInformation().setDataRights(dataRights);
        pdi.getAccesRightInformation().setPublicReleaseDate(publicReleaseDate);
        pdi.getAccesRightInformation().setLicence(licence);
    }

    /**
     * Alias for {@link PDIBuilder#setAccessRightInformation(String, String, OffsetDateTime)} (no public release
     * date)
     * @param dataRights secure key
     */
    public void setAccessRightInformation(String dataRights) {
        setAccessRightInformation(null, dataRights, null);
    }
}
