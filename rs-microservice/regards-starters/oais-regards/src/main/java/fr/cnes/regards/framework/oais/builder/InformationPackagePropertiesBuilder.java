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

    private final InformationPackageProperties ip;

    private final Set<ContentInformation> cis;

    private final PDIBuilder pdiBuilder;

    private final Map<String, Object> descriptiveInformation;

    private ContentInformationBuilder contentInformationBuilder;

    public InformationPackagePropertiesBuilder() {
        this.ip = new InformationPackageProperties();
        this.cis = Sets.newHashSet();
        this.contentInformationBuilder = new ContentInformationBuilder();
        this.pdiBuilder = new PDIBuilder();
        this.descriptiveInformation = Maps.newHashMap();
    }

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

    public void addTags(String... tags) {
        pdiBuilder.addTags(tags);
    }

    public void addContextInformation(String key, Object value) {
        pdiBuilder.addContextInformation(key, value);
    }

    public void addReferenceInformation(String key, String value) {
        pdiBuilder.addReferenceInformation(key, value);
    }

    public void addAdditionalProvenanceInformation(String key, Object value) {
        pdiBuilder.addAdditionalProvenanceInformation(key, value);
    }

    public void setFacility(String facility) {
        pdiBuilder.setFacility(facility);
    }

    public void setInstrument(String instrument) {
        pdiBuilder.setInstrument(instrument);
    }

    public void setFilter(String filter) {
        pdiBuilder.setFilter(filter);
    }

    public void setDetector(String detector) {
        pdiBuilder.setDetector(detector);
    }

    public void setProposal(String proposal) {
        pdiBuilder.setProposal(proposal);
    }

    public void addProvenanceInformationEvents(Event... events) {
        pdiBuilder.addProvenanceInformationEvents(events);
    }

    public void addProvenanceInformationEvent(@Nullable String type, String comment, OffsetDateTime date) {
        pdiBuilder.addProvenanceInformationEvent(type, comment, date);
    }

    public void addProvenanceInformationEvent(String comment, OffsetDateTime date) {
        pdiBuilder.addProvenanceInformationEvent(comment, date);
    }

    public void addProvenanceInformationEvent(String comment) {
        pdiBuilder.addProvenanceInformationEvent(comment);
    }

    public void addFixityInformation(String key, Object value) {
        pdiBuilder.addFixityInformation(key, value);
    }

    public void setAccessRightInformation(String licence, String dataRights,
            @Nullable OffsetDateTime publicReleaseDate) {
        pdiBuilder.setAccessRightInformation(licence, dataRights, publicReleaseDate);
    }

    public void setAccessRightInformation(String dataRights) {
        pdiBuilder.setAccessRightInformation(dataRights);
    }

    public void setDataObject(DataType dataType, URL url, String filename, String algorithm, String checksum,
            Long fileSize) {
        contentInformationBuilder.setDataObject(dataType, url, filename, algorithm, checksum, fileSize);
    }

    public void setDataObject(DataType dataType, Path filePath, String filename, String algorithm, String checksum,
            Long fileSize) {
        contentInformationBuilder.setDataObject(dataType, filePath, filename, algorithm, checksum, fileSize);
    }

    public void setDataObject(DataType dataType, URL url, String filename, String checksum, Long fileSize) {
        contentInformationBuilder.setDataObject(dataType, url, filename, checksum, fileSize);
    }

    public void setDataObject(DataType dataType, Path filePath, String filename, String checksum, Long fileSize) {
        contentInformationBuilder.setDataObject(dataType, filePath, filename, checksum, fileSize);
    }

    public void setDataObject(DataType dataType, URL url, String algorithm, String checksum) {
        contentInformationBuilder.setDataObject(dataType, url, algorithm, checksum);
    }

    public void setDataObject(DataType dataType, Path filePath, String algorithm, String checksum) {
        contentInformationBuilder.setDataObject(dataType, filePath, algorithm, checksum);
    }

    public void setDataObject(DataType dataType, URL url, String checksum) {
        contentInformationBuilder.setDataObject(dataType, url, checksum);
    }

    public void setDataObject(DataType dataType, Path filePath, String checksum) {
        contentInformationBuilder.setDataObject(dataType, filePath, checksum);
    }

    public void setSyntax(String mimeName, String mimeDescription, String mimeType) {
        contentInformationBuilder.setSyntax(mimeName, mimeDescription, mimeType);
    }

    public void setSyntaxAndSemantic(String mimeName, String mimeDescription, String mimeType,
            String semanticDescription) {
        contentInformationBuilder.setSyntaxAndSemantic(mimeName, mimeDescription, mimeType, semanticDescription);
    }

    public void addSoftwareEnvironmentProperty(String key, Object value) {
        contentInformationBuilder.addSoftwareEnvironmentProperty(key, value);
    }

    public void addHardwareEnvironmentProperty(String key, Object value) {
        contentInformationBuilder.addHardwareEnvironmentProperty(key, value);
    }

    public void removeTags(String... tags) {
        pdiBuilder.removeTags(tags);
    }
}
