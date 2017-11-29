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
import java.net.URL;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.InformationPackageProperties;
import fr.cnes.regards.framework.oais.PreservationDescriptionInformation;
import fr.cnes.regards.framework.oais.urn.DataType;

/**
 * Information package properties builder
 *
 * @author Marc Sordi
 *
 */
public class InformationPackagePropertiesBuilder implements IOAISBuilder<InformationPackageProperties> {

    /**
     * Information package properties
     */
    private final InformationPackageProperties ip;

    /**
     * Content information
     */
    private final Set<ContentInformation> cis;

    /**
     * Preservation and description information builder
     */
    private final PDIBuilder pdiBuilder;

    /**
     * Descriptive information
     */
    private final Map<String, Object> descriptiveInformation;

    /**
     * Content information builder
     */
    private ContentInformationBuilder contentInformationBuilder;

    /**
     * Default constructor
     */
    public InformationPackagePropertiesBuilder() {
        this.ip = new InformationPackageProperties();
        this.cis = Sets.newHashSet();
        this.contentInformationBuilder = new ContentInformationBuilder();
        this.pdiBuilder = new PDIBuilder();
        this.descriptiveInformation = Maps.newHashMap();
    }

    /**
     * Constructor initializing the builder with the given properties
     * @param properties
     */
    public InformationPackagePropertiesBuilder(InformationPackageProperties properties) {
        this.ip = properties;
        this.cis = properties.getContentInformations();
        this.pdiBuilder = new PDIBuilder(properties.getPdi());
        if (properties.getDescriptiveInformation() == null) {
            this.descriptiveInformation = Maps.newHashMap();
        } else {
            this.descriptiveInformation = properties.getDescriptiveInformation();
        }
    }

    @Override
    public InformationPackageProperties build() {
        ip.getContentInformations().addAll(cis);
        ip.setPdi(pdiBuilder.build());
        ip.getDescriptiveInformation().putAll(descriptiveInformation);
        return ip;
    }

    /**
     * Build content information from the content information builder and add it to the set of content informations of
     * this information package being built
     */
    public void addContentInformation() {
        cis.add(contentInformationBuilder.build());
        contentInformationBuilder = new ContentInformationBuilder();
    }

    /**
     * Add description information to the information package thanks to the given parameters
     * @param key
     * @param value
     */
    public void addDescriptiveInformation(String key, Object value) {
        Assert.hasLength(key, "Descriptive information key is required");
        Assert.notNull(value, "Descriptive information value is required");
        descriptiveInformation.put(key, value);
    }

    /**
     * @return builder for building <b>required</b> {@link ContentInformation}
     */
    public ContentInformationBuilder getContentInformationBuilder() {
        return contentInformationBuilder;
    }

    /**
     * @return builder for <b>required</b> {@link PreservationDescriptionInformation}
     */
    public PDIBuilder getPDIBuilder() {
        return pdiBuilder;
    }

    /**
     * Add tags to the information package
     * @param tags
     */
    public void addTags(String... tags) {
        pdiBuilder.addTags(tags);
    }

    /**
     * Add context information to the information package thanks to the given properties
     * @param key
     * @param value
     */
    public void addContextInformation(String key, Object value) {
        pdiBuilder.addContextInformation(key, value);
    }

    /**
     * Add reference information to the information package thanks to the given properties
     * @param key
     * @param value
     */
    public void addReferenceInformation(String key, String value) {
        pdiBuilder.addReferenceInformation(key, value);
    }

    /**
     * Add additional provenance information to the information package thanks to the given properties
     * @param key
     * @param value
     */
    public void addAdditionalProvenanceInformation(String key, Object value) {
        pdiBuilder.addAdditionalProvenanceInformation(key, value);
    }

    /**
     * Set the facility to the information package
     * @param facility
     */
    public void setFacility(String facility) {
        pdiBuilder.setFacility(facility);
    }

    /**
     * Set the instrument to the information package
     * @param instrument
     */
    public void setInstrument(String instrument) {
        pdiBuilder.setInstrument(instrument);
    }

    /**
     * Set the filter to the information package
     * @param filter
     */
    public void setFilter(String filter) {
        pdiBuilder.setFilter(filter);
    }

    /**
     * Set the detector to the information package
     * @param detector
     */
    public void setDetector(String detector) {
        pdiBuilder.setDetector(detector);
    }

    /**
     * Set the proposal to the information package
     * @param proposal
     */
    public void setProposal(String proposal) {
        pdiBuilder.setProposal(proposal);
    }

    /**
     * Add provenance information events to the information package
     * @param events
     */
    public void addProvenanceInformationEvents(Event... events) {
        pdiBuilder.addProvenanceInformationEvents(events);
    }

    /**
     * Add provenance information event to the information package thanks to the given parameters
     * @param type
     * @param comment
     * @param date
     */
    public void addProvenanceInformationEvent(@Nullable String type, String comment, OffsetDateTime date) {
        pdiBuilder.addProvenanceInformationEvent(type, comment, date);
    }

    /**
     * Add provenance information event to the information package thanks to the given parameters
     * @param comment
     * @param date
     */
    public void addProvenanceInformationEvent(String comment, OffsetDateTime date) {
        pdiBuilder.addProvenanceInformationEvent(comment, date);
    }

    /**
     * Add provenance information event to the information package thanks to the given parameter
     * @param comment
     */
    public void addProvenanceInformationEvent(String comment) {
        pdiBuilder.addProvenanceInformationEvent(comment);
    }

    /**
     * Add fixity information to the information package thanks to the given parameters
     * @param key
     * @param value
     */
    public void addFixityInformation(String key, Object value) {
        pdiBuilder.addFixityInformation(key, value);
    }

    /**
     * Set the access right information to the information package thanks to the given parameters
     * @param licence
     * @param dataRights
     * @param publicReleaseDate
     */
    public void setAccessRightInformation(String licence, String dataRights,
            @Nullable OffsetDateTime publicReleaseDate) {
        pdiBuilder.setAccessRightInformation(licence, dataRights, publicReleaseDate);
    }

    /**
     * Set the access right information to the information package thanks to the given parameter
     * @param dataRights
     */
    public void setAccessRightInformation(String dataRights) {
        pdiBuilder.setAccessRightInformation(dataRights);
    }

    /**
     * Set the data object to the information package thanks to the underlying content information builder using the given parameters
     * @param dataType
     * @param url
     * @param filename
     * @param algorithm
     * @param checksum
     * @param fileSize
     */
    public void setDataObject(DataType dataType, URL url, String filename, String algorithm, String checksum,
            Long fileSize) {
        contentInformationBuilder.setDataObject(dataType, url, filename, algorithm, checksum, fileSize);
    }

    /**
     * Set the data object to the information package thanks to the underlying content information builder using the given parameters
     * @param dataType
     * @param filename
     * @param algorithm
     * @param checksum
     * @param fileSize
     */
    public void setDataObject(DataType dataType, Path filePath, String filename, String algorithm, String checksum,
            Long fileSize) {
        contentInformationBuilder.setDataObject(dataType, filePath, filename, algorithm, checksum, fileSize);
    }

    /**
     * Set the data object to the information package thanks to the underlying content information builder using the given parameters
     * @param dataType
     * @param url
     * @param filename
     * @param checksum
     * @param fileSize
     */
    public void setDataObject(DataType dataType, URL url, String filename, String checksum, Long fileSize) {
        contentInformationBuilder.setDataObject(dataType, url, filename, checksum, fileSize);
    }

    /**
     * Set the data object to the information package thanks to the underlying content information builder using the given parameters
     * @param dataType
     * @param filename
     * @param checksum
     * @param fileSize
     */
    public void setDataObject(DataType dataType, Path filePath, String filename, String checksum, Long fileSize) {
        contentInformationBuilder.setDataObject(dataType, filePath, filename, checksum, fileSize);
    }

    /**
     * Set the data object to the information package thanks to the underlying content information builder using the given parameters
     * @param dataType
     * @param url
     * @param algorithm
     * @param checksum
     */
    public void setDataObject(DataType dataType, URL url, String algorithm, String checksum) {
        contentInformationBuilder.setDataObject(dataType, url, algorithm, checksum);
    }

    /**
     * Set the data object to the information package thanks to the underlying content information builder using the given parameters
     * @param dataType
     * @param algorithm
     * @param checksum
     */
    public void setDataObject(DataType dataType, Path filePath, String algorithm, String checksum) {
        contentInformationBuilder.setDataObject(dataType, filePath, algorithm, checksum);
    }

    /**
     * Set the data object to the information package thanks to the underlying content information builder using the given parameters
     * @param dataType
     * @param url
     * @param checksum
     */
    public void setDataObject(DataType dataType, URL url, String checksum) {
        contentInformationBuilder.setDataObject(dataType, url, checksum);
    }

    /**
     * Set the data object to the information package thanks to the underlying content information builder using the given parameters
     * @param dataType
     * @param checksum
     */
    public void setDataObject(DataType dataType, Path filePath, String checksum) {
        contentInformationBuilder.setDataObject(dataType, filePath, checksum);
    }

    /**
     * Set the syntax to the information package thanks to the given parameters
     * @param mimeName
     * @param mimeDescription
     * @param mimeType
     */
    public void setSyntax(String mimeName, String mimeDescription, String mimeType) {
        contentInformationBuilder.setSyntax(mimeName, mimeDescription, mimeType);
    }

    /**
     * Set the syntax and semantic to the information package thanks to the given parameters
     * @param mimeName
     * @param mimeDescription
     * @param mimeType
     * @param semanticDescription
     */
    public void setSyntaxAndSemantic(String mimeName, String mimeDescription, String mimeType,
            String semanticDescription) {
        contentInformationBuilder.setSyntaxAndSemantic(mimeName, mimeDescription, mimeType, semanticDescription);
    }

    /**
     * Add software environment property to the information package thanks to the given parameters
     * @param key
     * @param value
     */
    public void addSoftwareEnvironmentProperty(String key, Object value) {
        contentInformationBuilder.addSoftwareEnvironmentProperty(key, value);
    }


    /**
     * Add hardware environment property to the information package thanks to the given parameters
     * @param key
     * @param value
     */
    public void addHardwareEnvironmentProperty(String key, Object value) {
        contentInformationBuilder.addHardwareEnvironmentProperty(key, value);
    }

    /**
     * Remove tags from the information package
     * @param tags
     */
    public void removeTags(String... tags) {
        pdiBuilder.removeTags(tags);
    }
}
