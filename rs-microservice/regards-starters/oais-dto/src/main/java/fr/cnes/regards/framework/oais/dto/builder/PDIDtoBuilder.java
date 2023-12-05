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
package fr.cnes.regards.framework.oais.dto.builder;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.oais.dto.EventDto;
import fr.cnes.regards.framework.oais.dto.InformationPackageProperties;
import fr.cnes.regards.framework.oais.dto.PreservationDescriptionInformationDto;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Collection;

/**
 * Preservation Description Information Builder.<br/>
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
 * <li>{@link PDIDtoBuilder#addTags(String...)}</li>
 * <li>{@link #addContextCategories(String...)}</li>
 * <li>{@link PDIDtoBuilder#addContextInformation(String, Object)}</li>
 * </ul>
 * <br/>
 * The reference information object stores identifiers. <br/>
 * Method to use :
 * <ul>
 * <li>{@link PDIDtoBuilder#addReferenceInformation(String, String)}</li>
 * </ul>
 * <br/>
 * The provenance information object may store as indicated provenance information plus history events. <br/>
 * Methods to use :
 * <ul>
 * <li>{@link PDIDtoBuilder#setFacility(String)}</li>
 * <li>{@link PDIDtoBuilder#setInstrument(String)}</li>
 * <li>{@link PDIDtoBuilder#setFilter(String)}</li>
 * <li>{@link PDIDtoBuilder#setDetector(String)}</li>
 * <li>{@link PDIDtoBuilder#setProposal(String)}</li>
 * <li>{@link PDIDtoBuilder#addAdditionalProvenanceInformation(String, Object)}</li>
 * <li>{@link PDIDtoBuilder#addProvenanceInformationEvent(String)}</li>
 * <li>{@link PDIDtoBuilder#addProvenanceInformationEvent(String, OffsetDateTime)}</li>
 * <li>{@link PDIDtoBuilder#addProvenanceInformationEvent(String, String, OffsetDateTime)}</li>
 * <li>{@link PDIDtoBuilder#addProvenanceInformationEvents(EventDto...)}</li>
 * </ul>
 * <br/>
 * The fixity information object may store data consistency information. <br/>
 * Method to use :
 * <ul>
 * <li>{@link PDIDtoBuilder#addFixityInformation(String, Object)}</li>
 * </ul>
 * <br/>
 * The access right information object may store as indicated access right information. <br/>
 * Methods to use :
 * <ul>
 * <li>{@link PDIDtoBuilder#setAccessRightInformation(String)}</li>
 * <li>{@link PDIDtoBuilder#setAccessRightInformation(String, String, OffsetDateTime)}</li>
 * </ul>
 * <br/>
 *
 * @author Marc Sordi
 * @deprecated {@link InformationPackageProperties} fluent API
 */
@Deprecated
public class PDIDtoBuilder implements IOAISBuilder<PreservationDescriptionInformationDto> {

    private static final String CONTEXT_INFO_TAGS_KEY = "tags";

    private static final String CONTEXT_INFO_CATEGORIES = "categories";

    /**
     * Preservation and description information
     */
    private final PreservationDescriptionInformationDto pdi;

    public PDIDtoBuilder() {
        this.pdi = new PreservationDescriptionInformationDto();
    }

    /**
     * Constructor using the given preservation and description information as a base
     */
    public PDIDtoBuilder(PreservationDescriptionInformationDto pdi) {
        this.pdi = pdi;
    }

    @Override
    public PreservationDescriptionInformationDto build() {
        return pdi;
    }

    /**
     * Add tags
     *
     * @param tags tags to add
     */
    public void addTags(String... tags) {
        Assert.notEmpty(tags, "Tag is required");
        @SuppressWarnings("unchecked")
        Collection<String> existingTags = (Collection<String>) pdi.getContextInformation().get(CONTEXT_INFO_TAGS_KEY);
        if (existingTags == null) {
            existingTags = Sets.newHashSet(tags);
            pdi.getContextInformation().put(CONTEXT_INFO_TAGS_KEY, existingTags);
        } else {
            for (String tag : tags) {
                if (!existingTags.contains(tag)) {
                    existingTags.add(tag);
                }
            }
        }
    }

    /**
     * Remove tags from the information package
     */
    @SuppressWarnings("unchecked")
    public void removeTags(String... tags) {
        Collection<String> existingTags = (Collection<String>) pdi.getContextInformation().get(CONTEXT_INFO_TAGS_KEY);
        if (existingTags != null) {
            existingTags.removeAll(Sets.newHashSet(tags));
        }
    }

    /**
     * Add an <b>optional</b> context information
     *
     * @param key   information key
     * @param value information
     */
    public void addContextInformation(String key, Object value) {
        Assert.hasLength(key, "Context information key is required");
        Assert.isTrue(!key.equalsIgnoreCase(CONTEXT_INFO_TAGS_KEY), "Tags must be added via dedicated method");
        Assert.isTrue(!key.equalsIgnoreCase(CONTEXT_INFO_CATEGORIES), "Categories must be added via dedicated method");
        Assert.notNull(value, "Context information value is required");
        pdi.getContextInformation().put(key, value);
    }

    /**
     * Add categories to context information (repeatable)
     *
     * @param categories list of category
     */
    public void addContextCategories(String... categories) {
        Assert.notEmpty(categories, "Categories are required");
        @SuppressWarnings("unchecked")
        Collection<String> existingCats = (Collection<String>) pdi.getContextInformation().get(CONTEXT_INFO_CATEGORIES);
        if (existingCats == null) {
            existingCats = Sets.newHashSet(categories);
            pdi.getContextInformation().put(CONTEXT_INFO_CATEGORIES, existingCats);
        } else {
            for (String cat : categories) {
                if (!existingCats.contains(cat)) {
                    existingCats.add(cat);
                }
            }
        }
    }

    /**
     * Add optional reference information
     *
     * @param key   information key
     * @param value information
     */
    public void addReferenceInformation(String key, String value) {
        Assert.hasLength(key, "Reference information key is required");
        Assert.notNull(value, "Reference information value is required");
        pdi.getReferenceInformation().put(key, value);
    }

    /**
     * add additional provenance information
     *
     * @param key   name of the information
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
     *
     * @param events events to add
     */
    public void addProvenanceInformationEvents(EventDto... events) {
        Assert.notEmpty(events, "At least one event is required if this method is called");
        for (EventDto event : events) {
            Assert.hasLength(event.getComment(), "Event comment is required");
            Assert.notNull(event.getDate(), "Event date is required");
            pdi.getProvenanceInformation().addEvent(event.getType(), event.getComment(), event.getDate());
        }
    }

    /**
     * Add an information object event
     *
     * @param type    optional event type key (may be null)
     * @param comment event comment
     * @param date    event date
     */
    public void addProvenanceInformationEvent(@Nullable String type, String comment, OffsetDateTime date) {
        EventDto event = new EventDto();
        event.setType(type);
        event.setComment(comment);
        event.setDate(date);
        addProvenanceInformationEvents(event);
    }

    /**
     * Add an information object event
     *
     * @param comment event comment
     * @param date    event date
     */
    public void addProvenanceInformationEvent(String comment, OffsetDateTime date) {
        addProvenanceInformationEvent(null, comment, date);
    }

    /**
     * Add an information object event
     *
     * @param comment event comment
     */
    public void addProvenanceInformationEvent(String comment) {
        addProvenanceInformationEvent(null, comment, OffsetDateTime.now());
    }

    /**
     * Add an <b>optional</b> fixity information
     *
     * @param key   information key
     * @param value information
     */
    public void addFixityInformation(String key, Object value) {
        Assert.hasLength(key, "Fixity information key is required");
        Assert.notNull(value, "Fixity information value is required");
        pdi.getFixityInformation().put(key, value);
    }

    /**
     * Set <b>required</b> access right information
     *
     * @param licence           optional licence
     * @param dataRights        secure key
     * @param publicReleaseDate optional public release date (may be null)
     */
    public void setAccessRightInformation(String licence,
                                          String dataRights,
                                          @Nullable OffsetDateTime publicReleaseDate) {
        Assert.hasLength(dataRights, "Data rights is required");
        pdi.getAccessRightInformation().setDataRights(dataRights);
        pdi.getAccessRightInformation().setPublicReleaseDate(publicReleaseDate);
        pdi.getAccessRightInformation().setLicence(licence);
    }

    /**
     * Alias for {@link PDIDtoBuilder#setAccessRightInformation(String, String, OffsetDateTime)} (no public release
     * date)
     *
     * @param dataRights secure key
     */
    public void setAccessRightInformation(String dataRights) {
        setAccessRightInformation(null, dataRights, null);
    }

}
