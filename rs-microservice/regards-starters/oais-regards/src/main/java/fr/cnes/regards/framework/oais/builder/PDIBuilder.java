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

import org.springframework.util.Assert;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.PreservationDescriptionInformation;

/**
 *
 * Preservation Description Information Builder.<br/>
 *
 * A {@link PreservationDescriptionInformation} contains exactly five information objects :
 * <ul>
 * <li>A context information object</li>
 * <li>A reference information object</li>
 * <li>A provenance information object</li>
 * <li>A fixity information object</li>
 * <li>An access right information object</li>
 * </ul>
 * <hr>
 * This builder helps to fill in these objects.
 * <br/>
 * <br/>
 * The context information object may store links to other information outside (like tags). <br/>
 * Methods to use :
 * <ul>
 * <li>{@link PDIBuilder#addTags(String...)}</li>
 * <li>{@link PDIBuilder#addContextInformation(String, Object)}</li>
 * </ul>
 * <br/>
 * The reference information object stores identifiers. <br/>
 * Method to use :
 * <ul>
 * <li>{@link PDIBuilder#addReferenceInformation(String, String)}</li>
 * </ul>
 * <br/>
 * The provenance information object may store as indicated provenance information plus history events. <br/>
 * Methods to use :
 * <ul>
 * <li>{@link PDIBuilder#setFacility(String)}</li>
 * <li>{@link PDIBuilder#setInstrument(String)}</li>
 * <li>{@link PDIBuilder#setFilter(String)}</li>
 * <li>{@link PDIBuilder#setDetector(String)}</li>
 * <li>{@link PDIBuilder#setProposal(String)}</li>
 * <li>{@link PDIBuilder#addAdditionalProvenanceInformation(String, Object)}</li>
 * <li>{@link PDIBuilder#addProvenanceInformationEvent(String)}</li>
 * <li>{@link PDIBuilder#addProvenanceInformationEvent(String, OffsetDateTime)}</li>
 * <li>{@link PDIBuilder#addProvenanceInformationEvent(String, String, OffsetDateTime)}</li>
 * <li>{@link PDIBuilder#addProvenanceInformationEvents(Event...)}</li>
 * </ul>
 * <br/>
 * The fixity information object may store data consistency information. <br/>
 * Method to use :
 * <ul>
 * <li>{@link PDIBuilder#addFixityInformation(String, Object)}</li>
 * </ul>
 * <br/>
 * The access right information object may store as indicated access right information. <br/>
 * Methods to use :
 * <ul>
 * <li>{@link PDIBuilder#setAccessRightInformation(String)}</li>
 * <li>{@link PDIBuilder#setAccessRightInformation(String, String, OffsetDateTime)}</li>
 * </ul>
 * <br/>
 *
 * @author Marc Sordi
 *
 */
public class PDIBuilder implements IOAISBuilder<PreservationDescriptionInformation> {

    /**
     * Preservation and description information
     */
    private final PreservationDescriptionInformation pdi;

    public PDIBuilder() {
        this.pdi = new PreservationDescriptionInformation();
    }

    /**
     * Constructor using the given preservation and description information as a base
     * @param pdi
     */
    public PDIBuilder(PreservationDescriptionInformation pdi) {
        this.pdi = pdi;
    }

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
        @SuppressWarnings("unchecked") Collection<String> existingTags = (Collection<String>) pdi
                .getContextInformation().get(PreservationDescriptionInformation.CONTEXT_INFO_TAGS_KEY);
        if (existingTags == null) {
            existingTags = Sets.newHashSet(tags);
            pdi.getContextInformation().put(PreservationDescriptionInformation.CONTEXT_INFO_TAGS_KEY, existingTags);
        } else {
            existingTags.addAll(Arrays.asList(tags));
        }
    }

    /**
     * Remove tags from the information package
     * @param tags
     */
    public void removeTags(String... tags) {
        Collection<String> existingTags = (Collection<String>) pdi
                .getContextInformation().get(PreservationDescriptionInformation.CONTEXT_INFO_TAGS_KEY);
        if(existingTags != null) {
            existingTags.removeAll(Sets.newHashSet(tags));
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
     * Add optional reference information
     * @param key information key
     * @param value information
     */
    public void addReferenceInformation(String key, String value) {
        Assert.hasLength(key, "Reference information key is required");
        Assert.notNull(value, "Reference information value is required");
        pdi.getReferenceInformation().put(key, value);
    }

    /**
     * add additional provenance information
     * @param key name of the information
     * @param value value of the information
     */
    public void addAdditionalProvenanceInformation(String key, Object value) {
        Assert.hasLength(key, "Name of the additional provenance information is required");
        Assert.notNull(value, "Value of the additional provenance information is required");
        pdi.getProvenanceInformation().getAdditional().put(key, value);
    }

    /**
     * Set optional facility information
     */
    public void setFacility(String facility) {
        pdi.getProvenanceInformation().setFacility(facility);
    }

    /**
     * Set optional instrument information
     */
    public void setInstrument(String instrument) {
        pdi.getProvenanceInformation().setInstrument(instrument);
    }

    /**
     * Set optional filter information
     */
    public void setFilter(String filter) {
        pdi.getProvenanceInformation().setFilter(filter);
    }

    /**
     * Set optional detector information
     */
    public void setDetector(String detector) {
        pdi.getProvenanceInformation().setDetector(detector);
    }

    /**
     * Set optional proposal information
     */
    public void setProposal(String proposal) {
        pdi.getProvenanceInformation().setProposal(proposal);
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
            pdi.getProvenanceInformation().addEvent(event.getType(), event.getComment(), event.getDate());
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
     * Add an <b>optional</b> fixity information
     * @param key information key
     * @param value information
     */
    public void addFixityInformation(String key, Object value) {
        Assert.hasLength(key, "Fixity information key is required");
        Assert.notNull(value, "Fixity information value is required");
        pdi.getFixityInformation().put(key, value);
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
