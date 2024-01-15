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

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.urn.DataType;
import net.minidev.json.annotate.JsonIgnore;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Information package<br/>
 * <p>
 * An {@link InformationPackageProperties} contains :
 * <ul>
 * <li>An array of {@link ContentInformationDto} to describe related physical files</li>
 * <li>A {@link PreservationDescriptionInformationDto} object</li>
 * <li>A descriptive information object for any related metadata</li>
 * </ul>
 * <hr>
 * The fluent API helps to fulfil these objects.
 * <br/>
 * <br/>
 * To create a {@link ContentInformationDto}, use delegated methods :
 * <br/>
 * <br/>
 * To define the data object, use one of the following methods :
 * <ul>
 * <li>{@link #withDataObject(DataType, Path, String, String)}</li>
 * <li>{@link #withDataObject(DataType, String, String, String, Long, OAISDataObjectLocationDto...)}</li>
 * <li>{@link #withDataObject(DataType, Path, String, String, String, Long)}</li>
 * <li>{@link #withDataObject(DataType, Path, String)}</li>
 * <li>{@link #withDataObject(DataType, Path, String, String)}</li>
 * </ul>
 * <br/>
 * To set the representation information, use :
 * <ul>
 * <li>{@link #withSyntax(String, String, MimeType)}</li>
 * <li>{@link #withSyntax(MimeType)}</li>
 * <li>{@link #withSyntaxAndSemantic(String, String, MimeType, String)}</li>
 * <li>{@link #withHardwareEnvironmentProperty(String, Object)}</li>
 * <li>{@link #withSoftwareEnvironmentProperty(String, Object)}</li>
 * </ul>
 * <br/>
 * IMPORTANT thing, when {@link ContentInformationDto} is fulfilled, call {@link #registerContentInformation()} to register current one
 * and initialize a new one.
 * <br/>
 * <br/>
 * To fulfil {@link PreservationDescriptionInformationDto} object, use delegated methods :
 * <br/>
 * <br/>
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
 * To define descriptive information, just call {@link #withDescriptiveInformation(String, Object)}.
 *
 * @author Sylvain VISSIERE-GUERINET
 * @author Sébastien Binda
 */
public class InformationPackageProperties {

    /**
     * Only used to fulfil this object with fluent API
     */
    @GsonIgnore
    @JsonIgnore
    private ContentInformationDto underConstruction;

    /**
     * The content informations<br/>
     * Can be empty for metadata only information packages like datasets and collections.
     */
    @Valid
    private List<ContentInformationDto> contentInformations;

    /**
     * The preservation and description information
     */
    @NotNull(message = "Preservation description information is required")
    @Valid
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = PreservationDescriptionInformationDto.class)
    private PreservationDescriptionInformationDto pdi = new PreservationDescriptionInformationDto();

    /**
     * The descriptive information
     */
    private InformationPackageMapDto descriptiveInformation;

    /**
     * @return the content information
     */
    public List<ContentInformationDto> getContentInformations() {
        if (contentInformations == null) {
            contentInformations = new ArrayList<>();
        }
        return contentInformations;
    }

    public void setContentInformations(List<ContentInformationDto> contentInformations) {
        this.contentInformations = contentInformations;
    }

    public void setDescriptiveInformation(InformationPackageMapDto descriptiveInformation) {
        this.descriptiveInformation = descriptiveInformation;
    }

    /**
     * @return the preservation and description information
     */
    public PreservationDescriptionInformationDto getPdi() {
        return pdi;
    }

    /**
     * Set the preservation and description information
     */
    public void setPdi(PreservationDescriptionInformationDto pPdi) {
        pdi = pPdi;
    }

    /**
     * @return the descriptive information
     */
    public Map<String, Object> getDescriptiveInformation() {
        if (descriptiveInformation == null) {
            descriptiveInformation = new InformationPackageMapDto();
        }
        return descriptiveInformation;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + (contentInformations == null ? 0 : contentInformations.hashCode());
        result = (prime * result) + (descriptiveInformation == null ? 0 : descriptiveInformation.hashCode());
        result = (prime * result) + (pdi == null ? 0 : pdi.hashCode());
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
        final InformationPackageProperties other = (InformationPackageProperties) obj;
        if (contentInformations == null) {
            if (other.contentInformations != null) {
                return false;
            }
        } else if (!contentInformations.equals(other.contentInformations)) {
            return false;
        }
        if (descriptiveInformation == null) {
            if (other.descriptiveInformation != null) {
                return false;
            }
        } else if (!descriptiveInformation.equals(other.descriptiveInformation)) {
            return false;
        }
        if (pdi == null) {
            return other.pdi == null;
        } else {
            return pdi.equals(other.pdi);
        }
    }

    // Fluent API

    public static InformationPackageProperties build() {
        return new InformationPackageProperties();
    }

    private ContentInformationDto getUnderConstruction() {
        if (underConstruction == null) {
            underConstruction = new ContentInformationDto();
        }
        return underConstruction;
    }

    /**
     * Validate the under construction {@link ContentInformationDto} and add it to the set of content informations of
     * this information package being built
     */
    public void registerContentInformation() {
        if (underConstruction != null) {
            // Calling method prevents null check!
            if (!getContentInformations().contains(underConstruction)) {
                contentInformations.add(underConstruction);
            }
            // Init a new content information
            underConstruction = new ContentInformationDto();
        }
    }

    /**
     * Add description information to the information package thanks to the given parameters (repeatable)
     */
    public InformationPackageProperties withDescriptiveInformation(String key, Object value) {
        Assert.hasLength(key, "Descriptive information key is required");
        Assert.notNull(value, "Descriptive information value is required");
        getDescriptiveInformation().put(key, value);
        return this;
    }

    /**
     * Add description information to the information package thanks to the given parameters (repeatable)
     */
    public InformationPackageProperties withNullableDescriptiveInformation(String key, Object value) {
        Assert.hasLength(key, "Descriptive information key is required");
        getDescriptiveInformation().put(key, value);
        return this;
    }

    /**
     * Add tags to the information package (repeatable)
     */
    public InformationPackageProperties withContextTags(String... tags) {
        pdi.withContextTags(tags);
        return this;
    }

    /**
     * Add context information to the information package thanks to the given properties (repeatable)
     */
    public InformationPackageProperties withContextInformation(String key, Object value) {
        pdi.withContextInformation(key, value);
        return this;
    }

    /**
     * Add reference information to the information package thanks to the given properties (repeatable)
     */
    public InformationPackageProperties withReferenceInformation(String key, String value) {
        pdi.withReferenceInformation(key, value);
        return this;
    }

    /**
     * Add additional provenance information to the information package thanks to the given properties (repeatable)
     */
    public InformationPackageProperties withAdditionalProvenanceInformation(String key, Object value) {
        pdi.withAdditionalProvenanceInformation(key, value);
        return this;
    }

    /**
     * Set the facility to the information package
     */
    public InformationPackageProperties withFacility(String facility) {
        pdi.withFacility(facility);
        return this;
    }

    /**
     * Set the instrument to the information package
     */
    public InformationPackageProperties withInstrument(String instrument) {
        pdi.withInstrument(instrument);
        return this;
    }

    /**
     * Set the filter to the information package
     */
    public InformationPackageProperties withFilter(String filter) {
        pdi.withFilter(filter);
        return this;
    }

    /**
     * Set the detector to the information package
     */
    public InformationPackageProperties withDetector(String detector) {
        pdi.withDetector(detector);
        return this;
    }

    /**
     * Set the proposal to the information package
     */
    public InformationPackageProperties withProposal(String proposal) {
        pdi.withProposal(proposal);
        return this;
    }

    /**
     * Add provenance information events to the information package (repeatable)
     */
    public InformationPackageProperties withProvenanceInformationEvents(EventDto... events) {
        pdi.withProvenanceInformationEvents(events);
        return this;
    }

    /**
     * Add provenance information event to the information package thanks to the given parameters (repeatable)
     */
    public InformationPackageProperties withProvenanceInformationEvent(@Nullable String type,
                                                                       String comment,
                                                                       OffsetDateTime date) {
        pdi.withProvenanceInformationEvent(type, comment, date);
        return this;
    }

    /**
     * Add provenance information event to the information package thanks to the given parameters (repeatable)
     */
    public InformationPackageProperties withProvenanceInformationEvent(String comment, OffsetDateTime date) {
        pdi.withProvenanceInformationEvent(comment, date);
        return this;
    }

    /**
     * Add provenance information event to the information package thanks to the given parameter (repeatable)
     */
    public InformationPackageProperties withProvenanceInformationEvent(String comment) {
        pdi.withProvenanceInformationEvent(comment);
        return this;
    }

    /**
     * Add fixity information to the information package thanks to the given parameters
     */
    public InformationPackageProperties withFixityInformation(String key, Object value) {
        pdi.withFixityInformation(key, value);
        return this;
    }

    /**
     * Set the access right information to the information package thanks to the given parameters
     */
    public InformationPackageProperties withAccessRightInformation(String licence,
                                                                   String dataRights,
                                                                   @Nullable OffsetDateTime publicReleaseDate) {
        pdi.withAccessRightInformation(licence, dataRights, publicReleaseDate);
        return this;
    }

    /**
     * Set the access right information to the information package thanks to the given parameter
     */
    public InformationPackageProperties withAccessRightInformation(String dataRights) {
        pdi.withAccessRightInformation(dataRights);
        return this;
    }

    /**
     * Set <b>required</b> data object properties for a data object reference<br/>
     * Use this method to reference an external data object that will not be managed by archival storage (i.e. physical
     * file will not be stored by the system)<br/>
     *
     * @param dataType {@link DataType}
     * @param filename filename
     * @param url      external url
     * @param storage  storage identifier not managed by storage service (to just reference the file and avoid manipulating it).
     *                 An arbitrary character string may be appropriate!
     */
    public InformationPackageProperties withDataObjectReference(DataType dataType,
                                                                String filename,
                                                                String url,
                                                                String storage) {
        getUnderConstruction().withDataObjectReference(dataType, filename, url, storage);
        return this;
    }

    /**
     * Set <b>required</b> data object properties<br/>
     *
     * @param dataType  {@link DataType}
     * @param filename  filename
     * @param algorithm checksum algorithm
     * @param checksum  the checksum
     * @param fileSize  <b>optional</b> file size
     * @param locations references to the physical file. Use {@link OAISDataObjectLocationDto} build methods to create location!
     */
    public InformationPackageProperties withDataObject(DataType dataType,
                                                       String filename,
                                                       String algorithm,
                                                       String checksum,
                                                       Long fileSize,
                                                       OAISDataObjectLocationDto... locations) {
        getUnderConstruction().withDataObject(dataType, filename, algorithm, checksum, fileSize, locations);
        return this;
    }

    /**
     * Set <b>required</b> data object properties
     *
     * @param dataType  {@link DataType}
     * @param filePath  reference to the physical file
     * @param filename  filename
     * @param algorithm checksum algorithm
     * @param checksum  the checksum
     * @param fileSize  file size
     */
    public InformationPackageProperties withDataObject(DataType dataType,
                                                       Path filePath,
                                                       String filename,
                                                       String algorithm,
                                                       String checksum,
                                                       Long fileSize) {
        getUnderConstruction().withDataObject(dataType, filePath, filename, algorithm, checksum, fileSize);
        return this;
    }

    /**
     * Alias for {@link ContentInformationDto#withDataObject(DataType, Path, String, String, String, Long)} (no
     * file size)
     *
     * @param dataType  {@link DataType}
     * @param filePath  reference to the physical file
     * @param algorithm checksum algorithm
     * @param checksum  the checksum
     */
    public InformationPackageProperties withDataObject(DataType dataType,
                                                       Path filePath,
                                                       String algorithm,
                                                       String checksum) {
        getUnderConstruction().withDataObject(dataType, filePath, algorithm, checksum);
        return this;
    }

    /**
     * Alias for {@link ContentInformationDto#withDataObject(DataType, Path, String, String, String, Long)} (no file
     * size and MD5 default checksum algorithm)
     *
     * @param dataType {@link DataType}
     * @param filePath reference to the physical file
     * @param checksum the checksum
     */
    public InformationPackageProperties withDataObject(DataType dataType, Path filePath, String checksum) {
        getUnderConstruction().withDataObject(dataType, filePath, checksum);
        return this;
    }

    /**
     * Set the syntax to the information package thanks to the given parameters
     */
    public InformationPackageProperties withSyntax(String mimeName, String mimeDescription, MimeType mimeType) {
        getUnderConstruction().withSyntax(mimeName, mimeDescription, mimeType, null, null);
        return this;
    }

    /**
     * Set syntax representation
     *
     * @param mimeType MIME type
     */
    public InformationPackageProperties withSyntax(MimeType mimeType) {
        getUnderConstruction().withSyntax(mimeType);
        return this;
    }

    /**
     * Set syntax representation
     *
     * @param mimeType MIME type
     */
    public InformationPackageProperties withSyntaxAndDimension(MimeType mimeType, Double width, Double height) {
        getUnderConstruction().withSyntaxAndDimension(mimeType, width, height);
        return this;
    }

    /**
     * Set the syntax and semantic to the information package thanks to the given parameters
     */
    public InformationPackageProperties withSyntaxAndSemantic(@Nullable String mimeName,
                                                              @Nullable String mimeDescription,
                                                              MimeType mimeType,
                                                              String semanticDescription) {
        getUnderConstruction().withSyntaxAndSemantic(mimeName, mimeDescription, mimeType, semanticDescription);
        return this;
    }

    /**
     * Add software environment property to the information package thanks to the given parameters (repeatable)
     */
    public InformationPackageProperties withSoftwareEnvironmentProperty(String key, Object value) {
        getUnderConstruction().withSoftwareEnvironmentProperty(key, value);
        return this;
    }

    /**
     * Add hardware environment property to the information package thanks to the given parameters (repeatable)
     */
    public InformationPackageProperties withHardwareEnvironmentProperty(String key, Object value) {
        getUnderConstruction().withHardwareEnvironmentProperty(key, value);
        return this;
    }

    /**
     * Remove tags from the information package
     */
    public InformationPackageProperties withoutContextTags(String... tags) {
        pdi.withoutContextTags(tags);
        return this;
    }

}
