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
package fr.cnes.regards.framework.oais.builder;

import java.net.URL;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import com.google.common.collect.Maps;

import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.InformationPackageProperties;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.framework.oais.PreservationDescriptionInformation;
import fr.cnes.regards.framework.oais.urn.DataType;

/**
 * Information package properties builder
 * @author Marc Sordi
 *
 * @deprecated {@link InformationPackageProperties} fluent API
 */
@Deprecated
public class InformationPackagePropertiesBuilder implements IOAISBuilder<InformationPackageProperties> {

    /**
     * Information package properties
     */
    private final InformationPackageProperties ip;

    /**
     * Content information
     */
    private final List<ContentInformation> cis;

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
        this.cis = new ArrayList<>();
        this.contentInformationBuilder = new ContentInformationBuilder();
        this.pdiBuilder = new PDIBuilder();
        this.descriptiveInformation = Maps.newHashMap();
    }

    /**
     * Constructor initializing the builder with the given properties
     */
    public InformationPackagePropertiesBuilder(InformationPackageProperties properties) {
        this.ip = properties;
        this.cis = properties.getContentInformations();
        this.contentInformationBuilder = new ContentInformationBuilder();
        this.pdiBuilder = new PDIBuilder(properties.getPdi());
        this.descriptiveInformation = properties.getDescriptiveInformation();
    }

    @Override
    public InformationPackageProperties build() {
        addCis(cis);
        ip.setPdi(pdiBuilder.build());
        ip.getDescriptiveInformation().putAll(descriptiveInformation);
        return ip;
    }

    private void addCis(List<ContentInformation> cis) {
        List<ContentInformation> actualCis = ip.getContentInformations();
        for (ContentInformation ci : cis) {
            if (!actualCis.contains(ci)) {
                actualCis.add(ci);
            }
        }
    }

    /**
     * Build content information from the content information builder and add it to the set of content informations of
     * this information package being built
     */
    public void addContentInformation() {
        ContentInformation newCi = contentInformationBuilder.build();
        if (!cis.contains(newCi)) {
            cis.add(newCi);
        }
        contentInformationBuilder = new ContentInformationBuilder();
    }

    /**
     * Add description information to the information package thanks to the given parameters which can be null
     */
    public void addNullDescriptiveInformation(String key, Object value) {
        Assert.hasLength(key, "Descriptive information key is required");
        descriptiveInformation.put(key, value);
    }

    /**
     * Add description information to the information package thanks to the given parameters
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
     */
    public void addTags(String... tags) {
        pdiBuilder.addTags(tags);
    }

    /**
     * Add categories to context information (repeatable)
     * @param categories list of category
     */
    public void addContextCategories(String... categories) {
        pdiBuilder.addContextCategories(categories);
    }

    /**
     * Add context information to the information package thanks to the given properties
     */
    public void addContextInformation(String key, Object value) {
        pdiBuilder.addContextInformation(key, value);
    }

    /**
     * Add reference information to the information package thanks to the given properties
     */
    public void addReferenceInformation(String key, String value) {
        pdiBuilder.addReferenceInformation(key, value);
    }

    /**
     * Add additional provenance information to the information package thanks to the given properties
     */
    public void addAdditionalProvenanceInformation(String key, Object value) {
        pdiBuilder.addAdditionalProvenanceInformation(key, value);
    }

    /**
     * Set the facility to the information package
     */
    public void setFacility(String facility) {
        pdiBuilder.setFacility(facility);
    }

    /**
     * Set the instrument to the information package
     */
    public void setInstrument(String instrument) {
        pdiBuilder.setInstrument(instrument);
    }

    /**
     * Set the filter to the information package
     */
    public void setFilter(String filter) {
        pdiBuilder.setFilter(filter);
    }

    /**
     * Set the detector to the information package
     */
    public void setDetector(String detector) {
        pdiBuilder.setDetector(detector);
    }

    /**
     * Set the proposal to the information package
     */
    public void setProposal(String proposal) {
        pdiBuilder.setProposal(proposal);
    }

    /**
     * Add provenance information events to the information package
     */
    public void addProvenanceInformationEvents(Event... events) {
        pdiBuilder.addProvenanceInformationEvents(events);
    }

    /**
     * Add provenance information event to the information package thanks to the given parameters
     */
    public void addProvenanceInformationEvent(@Nullable String type, String comment, OffsetDateTime date) {
        pdiBuilder.addProvenanceInformationEvent(type, comment, date);
    }

    /**
     * Add provenance information event to the information package thanks to the given parameters
     */
    public void addProvenanceInformationEvent(String comment, OffsetDateTime date) {
        pdiBuilder.addProvenanceInformationEvent(comment, date);
    }

    /**
     * Add provenance information event to the information package thanks to the given parameter
     */
    public void addProvenanceInformationEvent(String comment) {
        pdiBuilder.addProvenanceInformationEvent(comment);
    }

    /**
     * Add fixity information to the information package thanks to the given parameters
     */
    public void addFixityInformation(String key, Object value) {
        pdiBuilder.addFixityInformation(key, value);
    }

    /**
     * Set the access right information to the information package thanks to the given parameters
     */
    public void setAccessRightInformation(String licence, String dataRights,
            @Nullable OffsetDateTime publicReleaseDate) {
        pdiBuilder.setAccessRightInformation(licence, dataRights, publicReleaseDate);
    }

    /**
     * Set the access right information to the information package thanks to the given parameter
     */
    public void setAccessRightInformation(String dataRights) {
        pdiBuilder.setAccessRightInformation(dataRights);
    }

    /**
     * Set <b>required</b> data object properties for a data object reference<br/>
     * Use this method to reference an external data object that will not be managed by archival storage (i.e. physical
     * file will not be stored by the system)<br/>
     * @param dataType {@link DataType}
     * @param filename filename
     * @param url external url
     * @param storage storage identifier not managed by storage service (to just reference the file and avoid manipulating it).
     * An arbitrary character string may be appropriate!
     */
    public void setDataObjectReference(DataType dataType, String filename, String url, String storage) {
        contentInformationBuilder.setDataObjectReference(dataType, filename, url, storage);
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
    public void setDataObject(DataType dataType, String filename, String algorithm, String checksum, Long fileSize,
            OAISDataObjectLocation... locations) {
        contentInformationBuilder.setDataObject(dataType, filename, algorithm, checksum, fileSize, locations);
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
    public void setDataObject(DataType dataType, Path filePath, String filename, String algorithm, String checksum,
            Long fileSize) {
        contentInformationBuilder.setDataObject(dataType, filePath, filename, algorithm, checksum, fileSize);
    }

    /**
     * Alias for {@link ContentInformationBuilder#setDataObject(DataType, Path, String, String, String, Long)} (no
     * file size)
     * @param dataType {@link DataType}
     * @param filePath reference to the physical file
     * @param algorithm checksum algorithm
     * @param checksum the checksum
     */
    public void setDataObject(DataType dataType, Path filePath, String algorithm, String checksum) {
        contentInformationBuilder.setDataObject(dataType, filePath, algorithm, checksum);
    }

    /**
     * Alias for {@link ContentInformationBuilder#setDataObject(DataType, Path, String, String, String, Long)} (no file
     * size and MD5 default checksum algorithm)
     * @param dataType {@link DataType}
     * @param filePath reference to the physical file
     * @param checksum the checksum
     */
    public void setDataObject(DataType dataType, Path filePath, String checksum) {
        contentInformationBuilder.setDataObject(dataType, filePath, checksum);
    }

    /**
     * Set the syntax to the information package thanks to the given parameters
     */
    public void setSyntax(String mimeName, String mimeDescription, MimeType mimeType) {
        contentInformationBuilder.setSyntax(mimeName, mimeDescription, mimeType);
    }

    /**
     * Set the syntax and semantic to the information package thanks to the given parameters
     */
    public void setSyntaxAndSemantic(String mimeName, String mimeDescription, MimeType mimeType,
            String semanticDescription) {
        contentInformationBuilder.setSyntaxAndSemantic(mimeName, mimeDescription, mimeType, semanticDescription);
    }

    /**
     * Add software environment property to the information package thanks to the given parameters
     */
    public void addSoftwareEnvironmentProperty(String key, Object value) {
        contentInformationBuilder.addSoftwareEnvironmentProperty(key, value);
    }

    /**
     * Add hardware environment property to the information package thanks to the given parameters
     */
    public void addHardwareEnvironmentProperty(String key, Object value) {
        contentInformationBuilder.addHardwareEnvironmentProperty(key, value);
    }

    /**
     * Remove tags from the information package
     */
    public void removeTags(String... tags) {
        pdiBuilder.removeTags(tags);
    }
}
