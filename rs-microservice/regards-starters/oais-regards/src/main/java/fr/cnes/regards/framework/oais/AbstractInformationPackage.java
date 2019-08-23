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

import java.net.URL;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import fr.cnes.regards.framework.geojson.AbstractFeature;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;

/**
 * OAIS Information package base structure
 *
 * An {@link InformationPackageProperties} contains :
 * <ul>
 * <li>An array of {@link ContentInformation} to describe related physical files</li>
 * <li>A {@link PreservationDescriptionInformation} object</li>
 * <li>A descriptive information object for any related metadata</li>
 * </ul>
 * <hr>
 * The fluent API helps to fulfil these objects.
 * <br/>
 * <br/>
 * To create a {@link ContentInformation}, use delegated methods :
 * <br/>
 * <br/>
 * To define the data object, use one of the following methods :
 * <ul>
 * <li>{@link #withDataObject(DataType, Path, String, String)}</li>
 * <li>{@link #withDataObject(DataType, String, String, String, Long, OAISDataObjectLocation...)}</li>
 * <li>{@link #withDataObject(DataType, Path, String, String, String, Long)}</li>
 * <li>{@link #withDataObject(DataType, Path, String)}</li>
 * <li>{@link #withDataObject(DataType, Path, String, String)}</li>
 * </ul>
 * <br/>
 * To set the representation information, use :
 * <ul>
 * <li>{@link #withSyntax(String, String, MimeType)}</li>
 * <li>{@link #withSyntaxAndSemantic(String, String, MimeType, String)}</li>
 * <li>{@link #withHardwareEnvironmentProperty(String, Object)}</li>
 * <li>{@link #withSoftwareEnvironmentProperty(String, Object)}</li>
 * </ul>
 * <br/>
 * IMPORTANT thing, when {@link ContentInformation} is fulfilled, call {@link #registerContentInformation()} to register current one
 * and initialize a new one. Do not call this method causes the object under construction to be forgotten!
 * <br/>
 * <br/>
 * To fulfil {@link PreservationDescriptionInformation} object, use delegated methods :
 * <br/>
 * <br/>
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
 * <li>{@link #withContextCategories(String...)}</li>
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
 * To define descriptive information, just call delegated method {@link #withDescriptiveInformation(String, Object)}.
 * <br/>
 * @author Marc Sordi
 * @author Sylvain Vissiere-Guerinet
 */
@SuppressWarnings("unchecked")
public abstract class AbstractInformationPackage<ID> extends AbstractFeature<InformationPackageProperties, ID> {

    @NotNull(message = "Information package type is required")
    private EntityType ipType;

    public AbstractInformationPackage() {
        this.properties = new InformationPackageProperties();
    }

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
        if (history.isEmpty()) {
            return null;
        } else {
            return history.get(history.size() - 1);
        }
    }

    public Event getSubmissionEvent() {
        return getHistory().stream().filter(e -> EventType.SUBMISSION.name().equals(e.getType())).findFirst()
                .orElse(null);
    }

    /**
     * @return the information package type
     */
    public EntityType getIpType() {
        return ipType;
    }

    public void setIpType(EntityType ipType) {
        this.ipType = ipType;
    }

    /**
     * Add Information package type comparison to AbstractFeature hashCode
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (ipType == null ? 0 : ipType.hashCode());
        return result;
    }

    /**
     * Add Information package type comparison to AbstractFeature equals
     */
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
        return ipType == other.ipType;
    }

    // Fluent API

    /**
     * Set required feature id and entity type
     * @param id feature id
     */
    public <T extends AbstractInformationPackage<ID>> T withIdAndType(ID id, EntityType type) {
        setId(id);
        setIpType(type);
        return (T) this;
    }

    /**
     * Set optional feature bounding box an CRS
     * @param bbox bounding box
     * @param crs coordinate reference system (default WGS 84)
     */

    public <T extends AbstractInformationPackage<ID>> T withBbox(Double[] bbox, String crs) {
        setBbox(bbox);
        setCrs(crs);
        return (T) this;
    }

    /**
     * Set optional feature bounding box
     */
    public <T extends AbstractInformationPackage<ID>> T withBbox(Double[] bbox) {
        withBbox(bbox, null);
        return (T) this;
    }

    /**
     * Set optional geometry. Use {@link IGeometry} to build a valid geometry.
     */
    public <T extends AbstractInformationPackage<ID>> T withGeometry(IGeometry geometry) {
        setGeometry(geometry);
        return (T) this;
    }

    /**
     * Register the newly constructed and fulfilled {@link ContentInformation} and add it to the set of content informations of
     * this information package
     */
    public <T extends AbstractInformationPackage<ID>> T registerContentInformation() {
        properties.registerContentInformation();
        return (T) this;
    }

    /**
     * Add <b>optional</b> descriptive information to the current information package. (repeatable)
     * @param key information key
     * @param value information value
     */
    public <T extends AbstractInformationPackage<ID>> T withDescriptiveInformation(String key, Object value) {
        properties.withDescriptiveInformation(key, value);
        return (T) this;
    }

    /**
     * Add tags into context information (repeatable)
     * @param tags list of tags
     */
    public <T extends AbstractInformationPackage<ID>> T withContextTags(String... tags) {
        properties.withContextTags(tags);
        return (T) this;
    }

    /**
     * Add link into context information (repeatable)
     * @param key link key
     * @param value link value
     */
    public <T extends AbstractInformationPackage<ID>> T withContextInformation(String key, Object value) {
        properties.withContextInformation(key, value);
        return (T) this;
    }

    /**
     * Add categories to context information (repeatable)
     * @param categories list of category
     */
    public <T extends AbstractInformationPackage<ID>> T withContextCategories(String... categories) {
        properties.withContextCategories(categories);
        return (T) this;
    }

    /**
     * Add reference information to the information package (repeatable)
     */
    public <T extends AbstractInformationPackage<ID>> T withReferenceInformation(String key, String value) {
        properties.withReferenceInformation(key, value);
        return (T) this;
    }

    /**
     * Add additional provenance information to the information package (repeatable)
     */
    public <T extends AbstractInformationPackage<ID>> T withAdditionalProvenanceInformation(String key, Object value) {
        properties.withAdditionalProvenanceInformation(key, value);
        return (T) this;
    }

    /**
     * Set the facility to the information package
     */
    public <T extends AbstractInformationPackage<ID>> T withFacility(String facility) {
        properties.withFacility(facility);
        return (T) this;
    }

    /**
     * Set the instrument to the information package
     */
    public <T extends AbstractInformationPackage<ID>> T withInstrument(String instrument) {
        properties.withInstrument(instrument);
        return (T) this;
    }

    /**
     * Set the filter to the information package
     */
    public <T extends AbstractInformationPackage<ID>> T withFilter(String filter) {
        properties.withFilter(filter);
        return (T) this;
    }

    /**
     * Set the detector to the information package
     */
    public <T extends AbstractInformationPackage<ID>> T withDetector(String detector) {
        properties.withDetector(detector);
        return (T) this;
    }

    /**
     * Set the proposal to the information package
     */
    public <T extends AbstractInformationPackage<ID>> T withProposal(String proposal) {
        properties.withProposal(proposal);
        return (T) this;
    }

    /**
     * Add provenance information events to the information package (repeatable)
     */
    public <T extends AbstractInformationPackage<ID>> T withProvenanceInformationEvents(Event... events) {
        properties.withProvenanceInformationEvents(events);
        return (T) this;
    }

    /**
     * Add a provenance information event to the information package thanks to the given parameters (repeatable)
     */
    public <T extends AbstractInformationPackage<ID>> T withProvenanceInformationEvent(@Nullable String type,
            String comment, OffsetDateTime date) {
        properties.withProvenanceInformationEvent(type, comment, date);
        return (T) this;
    }

    /**
     * Add a provenance information event to the information package thanks to the given parameters (repeatable)
     */
    public <T extends AbstractInformationPackage<ID>> T withProvenanceInformationEvent(String comment,
            OffsetDateTime date) {
        properties.withProvenanceInformationEvent(comment, date);
        return (T) this;
    }

    /**
     * Add a provenance information event to the information package thanks to the given parameter (repeatable)
     */
    public <T extends AbstractInformationPackage<ID>> T withProvenanceInformationEvent(String comment) {
        properties.withProvenanceInformationEvent(comment);
        return (T) this;
    }

    /**
     * Add fixity information to the information package thanks to the given parameters
     */
    public <T extends AbstractInformationPackage<ID>> T withFixityInformation(String key, Object value) {
        properties.withFixityInformation(key, value);
        return (T) this;
    }

    /**
     * Set the access right informtaion to the information package thanks to the given parameters
     */
    public <T extends AbstractInformationPackage<ID>> T withAccessRightInformation(String licence, String dataRights,
            @Nullable OffsetDateTime publicReleaseDate) {
        properties.withAccessRightInformation(licence, dataRights, publicReleaseDate);
        return (T) this;
    }

    /**
     * Set access right information to the information package thanks to the given parameter
     */
    public <T extends AbstractInformationPackage<ID>> T withAccessRightInformation(String dataRights) {
        properties.withAccessRightInformation(dataRights);
        return (T) this;
    }

    /**
     * Set <b>required</b> data object properties for a data object reference<br/>
     * Use this method to reference an external data object that will not be managed by archival storage (i.e. physical
     * file will not be stored by the system)<br/>
     * @param dataType {@link DataType}
     * @param filename filename
     * @param url external url
     * @param storage storage identifier not managed by storage service (to just reference the file and a<T extends AbstractInformationPackage<ID>> T manipulating it).
     * An arbitrary character string may be appropriate!
     */
    public <T extends AbstractInformationPackage<ID>> T withDataObjectReference(DataType dataType, String filename,
            URL url, String storage) {
        properties.withDataObjectReference(dataType, filename, url, storage);
        return (T) this;
    }

    /**
     * Set <b>required</b> data object properties<br/>
     * @param dataType {@link DataType}
     * @param filename filename
     * @param algorithm checksum algorithm
     * @param checksum the checksum
     * @param fileSize <b>optional</b> file size
     * @param locations references to the physical file. Use {@link OAISDataObjectLocation} build methods to create location!
     */
    public <T extends AbstractInformationPackage<ID>> T withDataObject(DataType dataType, String filename,
            String algorithm, String checksum, Long fileSize, OAISDataObjectLocation... locations) {
        properties.withDataObject(dataType, filename, algorithm, checksum, fileSize, locations);
        return (T) this;
    }

    /**
     * Set <b>required</b> data object properties
     * @param dataType {@link DataType}
     * @param filePath reference to the physical file
     * @param filename filename
     * @param algorithm checksum algorithm
     * @param checksum the checksum
     * @param fileSize file size
     */
    public <T extends AbstractInformationPackage<ID>> T withDataObject(DataType dataType, Path filePath,
            String filename, String algorithm, String checksum, Long fileSize) {
        properties.withDataObject(dataType, filePath, filename, algorithm, checksum, fileSize);
        return (T) this;
    }

    /**
     * Alias for {@link ContentInformation#withDataObject(DataType, Path, String, String, String, Long)} (no
     * file size)
     * @param dataType {@link DataType}
     * @param filePath reference to the physical file
     * @param algorithm checksum algorithm
     * @param checksum the checksum
     */
    public <T extends AbstractInformationPackage<ID>> T withDataObject(DataType dataType, Path filePath,
            String algorithm, String checksum) {
        properties.withDataObject(dataType, filePath, algorithm, checksum);
        return (T) this;
    }

    /**
     * Alias for {@link ContentInformation#withDataObject(DataType, Path, String, String, String, Long)} (no file
     * size and MD5 default checksum algorithm)
     * @param dataType {@link DataType}
     * @param filePath reference to the physical file
     * @param checksum the checksum
     */
    public <T extends AbstractInformationPackage<ID>> T withDataObject(DataType dataType, Path filePath,
            String checksum) {
        properties.withDataObject(dataType, filePath, checksum);
        return (T) this;
    }

    /**
     * Set the syntax to the information package thanks to the given parameters
     */
    public <T extends AbstractInformationPackage<ID>> T withSyntax(@Nullable String mimeName,
            @Nullable String mimeDescription, MimeType mimeType) {
        properties.withSyntax(mimeName, mimeDescription, mimeType);
        return (T) this;
    }

    /**
     * Set syntax representation
     * @param mimeType MIME type
     */
    public <T extends AbstractInformationPackage<ID>> T withSyntax(MimeType mimeType) {
        properties.withSyntax(mimeType);
        return (T) this;
    }

    /**
     * Set syntax and semantic to the information package thanks to the given parameters
     */
    public <T extends AbstractInformationPackage<ID>> T withSyntaxAndSemantic(String mimeName, String mimeDescription,
            MimeType mimeType, String semanticDescription) {
        properties.withSyntaxAndSemantic(mimeName, mimeDescription, mimeType, semanticDescription);
        return (T) this;
    }

    /**
     * Add software environment property to the information package thanks to the given parameters
     */
    public <T extends AbstractInformationPackage<ID>> T withSoftwareEnvironmentProperty(String key, Object value) {
        properties.withSoftwareEnvironmentProperty(key, value);
        return (T) this;
    }

    /**
     * Add hardware environment property to the information package thanks to the given parameters
     */
    public <T extends AbstractInformationPackage<ID>> T withHardwareEnvironmentProperty(String key, Object value) {
        properties.withHardwareEnvironmentProperty(key, value);
        return (T) this;
    }

    /**
     * Add IP events
     * @param events events to add
     */
    public <T extends AbstractInformationPackage<ID>> T withEvents(Event... events) {
        Assert.notEmpty(events, "At least one event is required if this method is called");
        properties.getPdi().withProvenanceInformationEvents(events);
        return (T) this;
    }

    /**
     * Add IP events
     * @param events events to add
     */
    public <T extends AbstractInformationPackage<ID>> T withEvents(Collection<Event> events) {
        Assert.notNull(events, "Collection of events cannot be null");
        return withEvents(events.toArray(new Event[0]));
    }

    /**
     * Add an IP event
     * @param type optional event type key (may be null)
     * @param comment event comment
     * @param date event date
     */
    public <T extends AbstractInformationPackage<ID>> T withEvent(@Nullable String type, String comment,
            OffsetDateTime date) {
        Event event = new Event();
        event.setType(type);
        event.setComment(comment);
        event.setDate(date);
        return withEvents(event);
    }

    /**
     * Add IP event
     * @param comment event comment
     * @param date event date
     */
    public <T extends AbstractInformationPackage<ID>> T withEvent(String comment, OffsetDateTime date) {
        return withEvent(null, comment, date);
    }

    /**
     * Add IP event
     * @param comment event comment
     */
    public <T extends AbstractInformationPackage<ID>> T withEvent(String comment) {
        return withEvent(null, comment, OffsetDateTime.now());
    }

    /**
     * Add IP event
     * @param comment event comment
     */
    public <T extends AbstractInformationPackage<ID>> T withEvent(String type, String comment) {
        return withEvent(type, comment, OffsetDateTime.now());
    }

    /**
     * Remove tags from the information package
     */
    public <T extends AbstractInformationPackage<ID>> T withoutContextTags(String... tags) {
        properties.withoutContextTags(tags);
        return (T) this;
    }
}
