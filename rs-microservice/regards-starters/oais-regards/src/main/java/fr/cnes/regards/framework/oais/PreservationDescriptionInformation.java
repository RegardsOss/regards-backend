/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.oais.adapter.InformationPackageMap;

/**
 * OAIS Preservation Description Information object<br/>
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
 * <li>{@link #withProvenanceInformationEvents(Event...)}</li>
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
 */
public class PreservationDescriptionInformation {

    public static final String CONTEXT_INFO_TAGS_KEY = "tags";

    /**
     * Should contains the tags too as a "special" key.
     */
    @NotNull(message = "Context information is required")
    private final InformationPackageMap contextInformation = new InformationPackageMap();

    @NotNull(message = "Reference information is required")
    private final Map<String, String> referenceInformation = new HashMap<>();

    @NotNull(message = "Provenance information is required")
    private final ProvenanceInformation provenanceInformation = new ProvenanceInformation();

    @NotNull(message = "Fixity information is required")
    private final InformationPackageMap fixityInformation = new InformationPackageMap();

    @NotNull(message = "Access right information is required")
    private final AccessRightInformation accessRightInformation = new AccessRightInformation();

    /**
     * @return the reference information
     */
    public Map<String, String> getReferenceInformation() {
        return referenceInformation;
    }

    /**
     * @return the context information
     */
    public Map<String, Object> getContextInformation() {
        return contextInformation;
    }

    /**
     * @return the tags
     */
    public Collection<String> getTags() {
        @SuppressWarnings("unchecked")
        Collection<String> tags = (Collection<String>) getContextInformation().get(CONTEXT_INFO_TAGS_KEY);
        if (tags == null) {
            tags = Sets.newHashSet();
            getContextInformation().put(CONTEXT_INFO_TAGS_KEY, tags);
        }
        return tags;
    }

    /**
     * @return the access right information
     */
    public AccessRightInformation getAccessRightInformation() {
        return accessRightInformation;
    }

    /**
     * @return the provenance information
     */
    public ProvenanceInformation getProvenanceInformation() {
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

        PreservationDescriptionInformation that = (PreservationDescriptionInformation) o;

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

    public static PreservationDescriptionInformation build() {
        return new PreservationDescriptionInformation();
    }

    /**
     * Add tags (repeatable)
     * @param tags tags to add
     */
    public PreservationDescriptionInformation withContextTags(String... tags) {
        Assert.notEmpty(tags, "Tag is required");
        @SuppressWarnings("unchecked")
        Collection<String> existingTags = (Collection<String>) getContextInformation()
                .get(PreservationDescriptionInformation.CONTEXT_INFO_TAGS_KEY);
        if (existingTags == null) {
            existingTags = Sets.newHashSet(tags);
            getContextInformation().put(PreservationDescriptionInformation.CONTEXT_INFO_TAGS_KEY, existingTags);
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
    public PreservationDescriptionInformation withoutContextTags(String... tags) {
        Collection<String> existingTags = (Collection<String>) getContextInformation().get(CONTEXT_INFO_TAGS_KEY);
        if (existingTags != null) {
            existingTags.removeAll(Sets.newHashSet(tags));
        }
        return this;
    }

    /**
     * Add an <b>optional</b> context information (repeatable)
     * @param key information key
     * @param value information
     */
    public PreservationDescriptionInformation withContextInformation(String key, Object value) {
        Assert.hasLength(key, "Context information key is required");
        Assert.isTrue(!key.equalsIgnoreCase(CONTEXT_INFO_TAGS_KEY), "Tags must be added via dedicated method");
        Assert.notNull(value, "Context information value is required");
        getContextInformation().put(key, value);
        return this;
    }

    /**
     * Add optional reference information (repeatable)
     * @param key information key
     * @param value information
     */
    public PreservationDescriptionInformation withReferenceInformation(String key, String value) {
        Assert.hasLength(key, "Reference information key is required");
        Assert.notNull(value, "Reference information value is required");
        getReferenceInformation().put(key, value);
        return this;
    }

    /**
     * add additional provenance information (repeatable)
     * @param key name of the information
     * @param value value of the information
     */
    public PreservationDescriptionInformation withAdditionalProvenanceInformation(String key, Object value) {
        Assert.hasLength(key, "Name of the additional provenance information is required");
        Assert.notNull(value, "Value of the additional provenance information is required");
        getProvenanceInformation().getAdditional().put(key, value);
        return this;
    }

    /**
     * Set optional facility information
     */
    public PreservationDescriptionInformation withFacility(String facility) {
        getProvenanceInformation().setFacility(facility);
        return this;
    }

    /**
     * Set optional instrument information
     */
    public PreservationDescriptionInformation withInstrument(String instrument) {
        getProvenanceInformation().setInstrument(instrument);
        return this;
    }

    /**
     * Set optional filter information
     */
    public PreservationDescriptionInformation withFilter(String filter) {
        getProvenanceInformation().setFilter(filter);
        return this;
    }

    /**
     * Set optional detector information
     */
    public PreservationDescriptionInformation withDetector(String detector) {
        getProvenanceInformation().setDetector(detector);
        return this;
    }

    /**
     * Set optional proposal information
     */
    public PreservationDescriptionInformation withProposal(String proposal) {
        getProvenanceInformation().setProposal(proposal);
        return this;
    }

    /**
     * Add information object events (repeatable)
     * @param events events to add
     */
    public PreservationDescriptionInformation withProvenanceInformationEvents(Event... events) {
        Assert.notEmpty(events, "At least one event is required if this method is called");
        for (Event event : events) {
            Assert.hasLength(event.getComment(), "Event comment is required");
            Assert.notNull(event.getDate(), "Event date is required");
            getProvenanceInformation().addEvent(event.getType(), event.getComment(), event.getDate());
        }
        return this;
    }

    /**
     * Add an information object event
     * @param type optional event type key (may be null)
     * @param comment event comment
     * @param date event date
     */
    public PreservationDescriptionInformation withProvenanceInformationEvent(@Nullable String type, String comment,
            OffsetDateTime date) {
        Event event = new Event();
        event.setType(type);
        event.setComment(comment);
        event.setDate(date);
        withProvenanceInformationEvents(event);
        return this;
    }

    /**
     * Add an information object event
     * @param comment event comment
     * @param date event date
     */
    public PreservationDescriptionInformation withProvenanceInformationEvent(String comment, OffsetDateTime date) {
        withProvenanceInformationEvent(null, comment, date);
        return this;
    }

    /**
     * Add an information object event
     * @param comment event comment
     */
    public PreservationDescriptionInformation withProvenanceInformationEvent(String comment) {
        withProvenanceInformationEvent(null, comment, OffsetDateTime.now());
        return this;
    }

    /**
     * Add an <b>optional</b> fixity information
     * @param key information key
     * @param value information
     */
    public PreservationDescriptionInformation withFixityInformation(String key, Object value) {
        Assert.hasLength(key, "Fixity information key is required");
        Assert.notNull(value, "Fixity information value is required");
        getFixityInformation().put(key, value);
        return this;
    }

    /**
     * Set <b>required</b> access right information
     * @param licence optional licence
     * @param dataRights secure key
     * @param publicReleaseDate optional public release date (may be null)
     */
    public PreservationDescriptionInformation withAccessRightInformation(String licence, String dataRights,
            @Nullable OffsetDateTime publicReleaseDate) {
        Assert.hasLength(dataRights, "Data rights is required");
        getAccessRightInformation().setDataRights(dataRights);
        getAccessRightInformation().setPublicReleaseDate(publicReleaseDate);
        getAccessRightInformation().setLicence(licence);
        return this;
    }

    /**
     * Alias for {@link PreservationDescriptionInformation#withAccessRightInformation(String, String, OffsetDateTime)} (no public release
     * date)
     * @param dataRights secure key
     */
    public PreservationDescriptionInformation withAccessRightInformation(String dataRights) {
        withAccessRightInformation(null, dataRights, null);
        return this;
    }
}
