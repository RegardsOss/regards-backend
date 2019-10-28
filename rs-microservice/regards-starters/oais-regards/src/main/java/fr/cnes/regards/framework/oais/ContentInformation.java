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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import fr.cnes.regards.framework.oais.urn.DataType;

/**
 * OAIS content information<br/>
 *
 * A {@link ContentInformation} is composed of two objects :
 * <ul>
 * <li>An {@link OAISDataObject} containing physical file information</li>
 * <li>A {@link RepresentationInformation} object describing how to handle, understand, etc. this data object.</li>
 * </ul>
 * <hr>
 * The fluent API helps to fulfil these objects.
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
 * <li>{@link #withSyntax(MimeType)}</li>
 * <li>{@link #withSyntaxAndSemantic(String, String, MimeType, String)}</li>
 * <li>{@link #withHardwareEnvironmentProperty(String, Object)}</li>
 * <li>{@link #withSoftwareEnvironmentProperty(String, Object)}</li>
 * </ul>
 * <br/>
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 */
public class ContentInformation {

    public static final String MD5_ALGORITHM = "MD5";

    @Valid
    @NotNull(message = "A representation information is required in content information")
    private RepresentationInformation representationInformation;

    @NotNull(message = "A data object is required in content information")
    @Valid
    private OAISDataObject dataObject;

    public OAISDataObject getDataObject() {
        return dataObject;
    }

    public void setDataObject(OAISDataObject pDataObject) {
        dataObject = pDataObject;
    }

    public RepresentationInformation getRepresentationInformation() {
        return representationInformation;
    }

    public void setRepresentationInformation(RepresentationInformation pRepresentationInformation) {
        representationInformation = pRepresentationInformation;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + (dataObject == null ? 0 : dataObject.hashCode());
        result = (prime * result) + (representationInformation == null ? 0 : representationInformation.hashCode());
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
        ContentInformation other = (ContentInformation) obj;
        if (dataObject == null) {
            if (other.dataObject != null) {
                return false;
            }
        } else if (!dataObject.equals(other.dataObject)) {
            return false;
        }
        if (representationInformation == null) {
            return other.representationInformation == null;
        } else {
            return representationInformation.equals(other.representationInformation);
        }
    }

    // Fluent API

    public static ContentInformation build() {
        return new ContentInformation();
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
    public ContentInformation withDataObjectReference(DataType dataType, String filename, String url, String storage) {
        Assert.notNull(dataType, "Data type is required");
        Assert.hasText(filename, "Filename is required");
        Assert.notNull(url, "URL is required");
        Assert.hasText(storage,
                       "Storage identifier is required (not managed by storage - not a plugin configuration business identifier");

        OAISDataObject dataObject = new OAISDataObject();
        dataObject.setRegardsDataType(dataType);
        dataObject.addLocation(OAISDataObjectLocation.build(url, storage));
        dataObject.setFilename(filename);
        setDataObject(dataObject);
        return this;
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
    public ContentInformation withDataObject(DataType dataType, String filename, String algorithm, String checksum,
            Long fileSize, OAISDataObjectLocation... locations) {
        Assert.notNull(dataType, "Data type is required");
        Assert.hasText(filename, "Filename is required");
        Assert.hasText(algorithm, "Checksum algorithm is required");
        Assert.hasText(checksum, "Checksum is required");
        Assert.notEmpty(locations, "At least one location is required");

        OAISDataObject dataObject = new OAISDataObject();
        dataObject.setFilename(filename);
        dataObject.setRegardsDataType(dataType);
        dataObject.setLocations(new HashSet<>(Arrays.asList(locations)));
        dataObject.setAlgorithm(algorithm);
        dataObject.setChecksum(checksum);
        dataObject.setFileSize(fileSize);
        setDataObject(dataObject);
        return this;
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
    public ContentInformation withDataObject(DataType dataType, Path filePath, String filename, String algorithm,
            String checksum, Long fileSize) {
        return withDataObject(dataType, filename, algorithm, checksum, fileSize,
                              OAISDataObjectLocation.build(filePath));
    }

    /**
     * Alias for {@link ContentInformation#withDataObject(DataType, Path, String, String, String, Long)} (no
     * file size)
     * @param dataType {@link DataType}
     * @param filePath reference to the physical file
     * @param algorithm checksum algorithm
     * @param checksum the checksum
     */
    public ContentInformation withDataObject(DataType dataType, Path filePath, String algorithm, String checksum) {
        return withDataObject(dataType, filePath, filePath.getFileName().toString(), algorithm, checksum, null);
    }

    /**
     * Alias for {@link ContentInformation#withDataObject(DataType, Path, String, String, String, Long)} (no file
     * size and MD5 default checksum algorithm)
     * @param dataType {@link DataType}
     * @param filePath reference to the physical file
     * @param checksum the checksum
     */
    public ContentInformation withDataObject(DataType dataType, Path filePath, String checksum) {
        return withDataObject(dataType, filePath, filePath.getFileName().toString(), MD5_ALGORITHM, checksum, null);
    }

    /**
     * Set syntax representation
     * @param mimeName MIME name
     * @param mimeDescription MIME description
     * @param mimeType MIME type
     */
    public ContentInformation withSyntax(@Nullable String mimeName, @Nullable String mimeDescription, MimeType mimeType,
            @Nullable Integer width, @Nullable Integer height) {
        Assert.notNull(mimeType, "Mime type cannot be null");
        Assert.hasLength(mimeType.getType(), "Mime type type cannot be null");
        Assert.hasLength(mimeType.getSubtype(), "Mime type subtype cannot be null");

        Syntax syntax = new Syntax();
        syntax.setName(mimeName);
        syntax.setDescription(mimeDescription);
        syntax.setMimeType(mimeType);
        syntax.setWidth(width);
        syntax.setHeight(height);

        setRepresentationInformation(new RepresentationInformation());
        getRepresentationInformation().setSyntax(syntax);
        return this;
    }

    /**
     * Set syntax representation
     * @param mimeType MIME type
     */
    public ContentInformation withSyntax(MimeType mimeType) {
        return withSyntax(null, null, mimeType, null, null);
    }

    /**
     * Set syntax representation
     * @param mimeType MIME type
     */
    public ContentInformation withSyntaxAndDimension(MimeType mimeType, Integer width, Integer height) {
        return withSyntax(null, null, mimeType, width, height);
    }

    /**
     * Set syntax and <b>optional</b> semantic representations
     * @param mimeName MIME name
     * @param mimeDescription MIME description
     * @param mimeType MIME type
     * @param semanticDescription semantic description
     */
    public ContentInformation withSyntaxAndSemantic(String mimeName, String mimeDescription, MimeType mimeType,
            String semanticDescription) {
        withSyntax(mimeName, mimeDescription, mimeType, null, null);

        Assert.hasLength(semanticDescription, "Semantic description cannot be null. Use alternative method otherwise.");
        Semantic semantic = new Semantic();
        semantic.setDescription(semanticDescription);

        getRepresentationInformation().setSemantic(semantic);
        return this;
    }

    /**
     * Set syntax and <b>optional</b> semantic representations
     * @param mimeType MIME type
     * @param semanticDescription semantic description
     */
    public ContentInformation withSyntaxAndSemantic(MimeType mimeType, String semanticDescription) {
        return withSyntaxAndSemantic(null, null, mimeType, semanticDescription);
    }

    /**
     * Add sofware Environment property with the given parameters (repeatable)
     */
    public ContentInformation withSoftwareEnvironmentProperty(String key, Object value) {
        Assert.hasLength(key, "Software environment information key is required");
        Assert.notNull(value, "Software environment information value is required");
        getRepresentationInformation().getEnvironmentDescription().getSoftwareEnvironment().put(key, value);
        return this;
    }

    /**
     * Add hardware environment property with the given parameters (repeatable)
     */
    public ContentInformation withHardwareEnvironmentProperty(String key, Object value) {
        Assert.hasLength(key, "Hardware environment information key is required");
        Assert.notNull(value, "Hardware environment information value is required");
        getRepresentationInformation().getEnvironmentDescription().getHardwareEnvironment().put(key, value);
        return this;
    }
}
