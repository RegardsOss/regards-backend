/*

 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Sets;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * OAIS Preservation Description Information object<br/>
 * <p>
 * A {@link PreservationDescriptionInformationDto} contains exactly five information objects :
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
 * <li>{@link #withContextTags(String...)}</li>
 * <li>{@link #withContextInformation(String, Object)}</li>
 * </ul>
 * <br/>
 * The reference information object stores identifiers. <br/>
 * Method to use :
 * <ul>
 * <li>{@link #withReferenceInformation(String, String)}</li>
 * </ul>
 * <br/>
 * The provenance information object may store as indicated provenance information plus history events. <br/>
 * Methods to use :
 * <ul>
 * <li>{@link #withFacility(String)}</li>
 * <li>{@link #withInstrument(String)}</li>
 * <li>{@link #withFilter(String)}</li>
 * <li>{@link #withDetector(String)}</li>
 * <li>{@link #withProposal(String)}</li>
 * <li>{@link #withAdditionalProvenanceInformation(String, Object)}</li>
 * <li>{@link #withProvenanceInformationEvent(String)}</li>
 * <li>{@link #withProvenanceInformationEvent(String, OffsetDateTime)}</li>
 * <li>{@link #withProvenanceInformationEvent(String, String, OffsetDateTime)}</li>
 * <li>{@link #withProvenanceInformationEvents(EventDto...)}</li>
 * </ul>
 * <br/>
 * The fixity information object may store data consistency information. <br/>
 * Method to use :
 * <ul>
 * <li>{@link #withFixityInformation(String, Object)}</li>
 * </ul>
 * <br/>
 * The access right information object may store as indicated access right information. <br/>
 * Methods to use :
 * <ul>
 * <li>{@link #withAccessRightInformation(String)}</li>
 * <li>{@link #withAccessRightInformation(String, String, OffsetDateTime)}</li>
 * </ul>
 * <br/>
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 * @author Michael Nguyen
 */
public class PreservationDescriptionInformationDto {

    public static final String CONTEXT_INFO_TAGS_KEY = "tags";

    /**
     * Should contains the tags too as a "special" key.
     */
    @NotNull(message = "Context information is required")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private InformationPackageMapDto contextInformation = new InformationPackageMapDto();

    @NotNull(message = "Reference information is required")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final ConcurrentMap<String, String> referenceInformation = new ConcurrentHashMap<>();

    @NotNull(message = "Provenance information is required")
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = ProvenanceInformationDto.class)
    private ProvenanceInformationDto provenanceInformation = new ProvenanceInformationDto();

    @NotNull(message = "Fixity information is required")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final InformationPackageMapDto fixityInformation = new InformationPackageMapDto();

    @NotNull(message = "Access right information is required")
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = AccessRightInformationDto.class)
    private final AccessRightInformationDto accessRightInformation = new AccessRightInformationDto();

    /**
     * @return the reference information
     */
    public Map<String, String> getReferenceInformation() {
        return referenceInformation;
    }

    public void setProvenanceInformation(ProvenanceInformationDto provenanceInformation) {
        this.provenanceInformation = provenanceInformation;
    }

    /**
     * @return the context information
     */
    public Map<String, Object> getContextInformation() {
        return contextInformation;
    }

    public void setContextInformation(InformationPackageMapDto contextInformation) {
        this.contextInformation = contextInformation;
    }

    /**
     * @return the tags
     */
    @JsonIgnore
    public Collection<String> getTags() {
        @SuppressWarnings("unchecked") Collection<String> tags = (Collection<String>) getContextInformation().get(
                CONTEXT_INFO_TAGS_KEY);
        if (tags == null) {
            tags = Sets.newHashSet();
            getContextInformation().put(CONTEXT_INFO_TAGS_KEY, tags);
        }
        return tags;
    }

    /**
     * @return the access right information
     */
    public AccessRightInformationDto getAccessRightInformation() {
        return accessRightInformation;
    }

    /**
     * @return the provenance information
     */
    public ProvenanceInformationDto getProvenanceInformation() {
        return provenanceInformation;
    }

    /**
     * @return the fixity information
     */
    public Map<String, Object> getFixityInformation() {
        return fixityInformation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PreservationDescriptionInformationDto that = (PreservationDescriptionInformationDto) o;

        if (!contextInformation.equals(that.contextInformation)) {
            return false;
        }
        if (!referenceInformation.equals(that.referenceInformation)) {
            return false;
        }
        if (!provenanceInformation.equals(that.provenanceInformation)) {
            return false;
        }
        if (!fixityInformation.equals(that.fixityInformation)) {
            return false;
        }
        return accessRightInformation.equals(that.accessRightInformation);
    }

    @Override
    public int hashCode() {
        int result = contextInformation.hashCode();
        result = 31 * result + referenceInformation.hashCode();
        result = 31 * result + provenanceInformation.hashCode();
        result = 31 * result + fixityInformation.hashCode();
        result = 31 * result + accessRightInformation.hashCode();
        return result;
    }

    // Fluent API

    public static PreservationDescriptionInformationDto build() {
        return new PreservationDescriptionInformationDto();
    }

    /**
     * Add tags (repeatable)
     *
     * @param tags tags to add
     */
    public PreservationDescriptionInformationDto withContextTags(String... tags) {
        Assert.notEmpty(tags, "Tag is required");
        @SuppressWarnings("unchecked")
        Collection<String> existingTags = (Collection<String>) getContextInformation().get(
                PreservationDescriptionInformationDto.CONTEXT_INFO_TAGS_KEY);
        if (existingTags == null) {
            existingTags = Sets.newHashSet(tags);
            getContextInformation().put(PreservationDescriptionInformationDto.CONTEXT_INFO_TAGS_KEY, existingTags);
        } else {
            for (String tag : tags) {
                if (!existingTags.contains(tag)) {
                    existingTags.add(tag);
                }
            }
        }
        return this;
    }

    /**
     * Remove tags from the information package
     */
    @SuppressWarnings("unchecked")
    public PreservationDescriptionInformationDto withoutContextTags(String... tags) {
        Collection<String> existingTags = (Collection<String>) getContextInformation().get(CONTEXT_INFO_TAGS_KEY);
        if (existingTags != null) {
            existingTags.removeAll(Sets.newHashSet(tags));
        }
        return this;
    }

    /**
     * Add an <b>optional</b> context information (repeatable)
     *
     * @param key   information key
     * @param value information
     */
    public PreservationDescriptionInformationDto withContextInformation(String key, Object value) {
        Assert.hasLength(key, "Context information key is required");
        Assert.isTrue(!key.equalsIgnoreCase(CONTEXT_INFO_TAGS_KEY), "Tags must be added via dedicated method");
        Assert.notNull(value, "Context information value is required");
        getContextInformation().put(key, value);
        return this;
    }

    /**
     * Add optional reference information (repeatable)
     *
     * @param key   information key
     * @param value information
     */
    public PreservationDescriptionInformationDto withReferenceInformation(String key, String value) {
        Assert.hasLength(key, "Reference information key is required");
        Assert.notNull(value, "Reference information value is required");
        getReferenceInformation().put(key, value);
        return this;
    }

    /**
     * add additional provenance information (repeatable)
     *
     * @param key   name of the information
     * @param value value of the information
     */
    public PreservationDescriptionInformationDto withAdditionalProvenanceInformation(String key, Object value) {
        Assert.hasLength(key, "Name of the additional provenance information is required");
        Assert.notNull(value, "Value of the additional provenance information is required");
        getProvenanceInformation().getAdditional().put(key, value);
        return this;
    }

    /**
     * Set optional facility information
     */
    public PreservationDescriptionInformationDto withFacility(String facility) {
        getProvenanceInformation().setFacility(facility);
        return this;
    }

    /**
     * Set optional instrument information
     */
    public PreservationDescriptionInformationDto withInstrument(String instrument) {
        getProvenanceInformation().setInstrument(instrument);
        return this;
    }

    /**
     * Set optional filter information
     */
    public PreservationDescriptionInformationDto withFilter(String filter) {
        getProvenanceInformation().setFilter(filter);
        return this;
    }

    /**
     * Set optional detector information
     */
    public PreservationDescriptionInformationDto withDetector(String detector) {
        getProvenanceInformation().setDetector(detector);
        return this;
    }

    /**
     * Set optional proposal information
     */
    public PreservationDescriptionInformationDto withProposal(String proposal) {
        getProvenanceInformation().setProposal(proposal);
        return this;
    }

    /**
     * Add information object events (repeatable)
     *
     * @param events events to add
     */
    public PreservationDescriptionInformationDto withProvenanceInformationEvents(EventDto... events) {
        Assert.notEmpty(events, "At least one event is required if this method is called");
        for (EventDto event : events) {
            Assert.hasLength(event.getComment(), "Event comment is required");
            Assert.notNull(event.getDate(), "Event date is required");
            getProvenanceInformation().addEvent(event.getType(), event.getComment(), event.getDate());
        }
        return this;
    }

    /**
     * Add an information object event
     *
     * @param type    optional event type key (may be null)
     * @param comment event comment
     * @param date    event date
     */
    public PreservationDescriptionInformationDto withProvenanceInformationEvent(@Nullable String type,
                                                                                String comment,
                                                                                OffsetDateTime date) {
        EventDto event = new EventDto();
        event.setType(type);
        event.setComment(comment);
        event.setDate(date);
        withProvenanceInformationEvents(event);
        return this;
    }

    /**
     * Add an information object event
     *
     * @param comment event comment
     * @param date    event date
     */
    public PreservationDescriptionInformationDto withProvenanceInformationEvent(String comment, OffsetDateTime date) {
        withProvenanceInformationEvent(null, comment, date);
        return this;
    }

    /**
     * Add an information object event
     *
     * @param comment event comment
     */
    public PreservationDescriptionInformationDto withProvenanceInformationEvent(String comment) {
        withProvenanceInformationEvent(null, comment, OffsetDateTime.now());
        return this;
    }

    /**
     * Add an <b>optional</b> fixity information
     *
     * @param key   information key
     * @param value information
     */
    public PreservationDescriptionInformationDto withFixityInformation(String key, Object value) {
        Assert.hasLength(key, "Fixity information key is required");
        Assert.notNull(value, "Fixity information value is required");
        getFixityInformation().put(key, value);
        return this;
    }

    /**
     * Set <b>required</b> access right information
     *
     * @param licence           optional licence
     * @param dataRights        secure key
     * @param publicReleaseDate optional public release date (may be null)
     */
    public PreservationDescriptionInformationDto withAccessRightInformation(String licence,
                                                                            String dataRights,
                                                                            @Nullable
                                                                            OffsetDateTime publicReleaseDate) {
        Assert.hasLength(dataRights, "Data rights is required");
        getAccessRightInformation().setDataRights(dataRights);
        getAccessRightInformation().setPublicReleaseDate(publicReleaseDate);
        getAccessRightInformation().setLicence(licence);
        return this;
    }

    /**
     * Alias for {@link PreservationDescriptionInformationDto #withAccessRightInformation(String, String, OffsetDateTime)} (no public release
     * date)
     *
     * @param dataRights secure key
     */
    public PreservationDescriptionInformationDto withAccessRightInformation(String dataRights) {
        withAccessRightInformation(null, dataRights, null);
        return this;
    }
}
