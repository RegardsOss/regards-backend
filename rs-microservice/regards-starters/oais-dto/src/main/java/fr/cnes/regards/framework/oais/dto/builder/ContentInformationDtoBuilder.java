/*

 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.oais.dto.*;
import fr.cnes.regards.framework.urn.DataType;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Content Information Builder.<br/>
 * <p>
 * A {@link ContentInformationDto} is composed of two objects :
 * <ul>
 * <li>An {@link OAISDataObjectDto} containing physical file information</li>
 * <li>A {@link RepresentationInformationDto} object describing how to handle,understand,etc. this data object.</li>
 * </ul>
 * <hr>
 * This builder helps to fill in these objects.
 * <br/>
 * <br/>
 * To define the data object, use one of the following methods :
 * <ul>
 * <li>{@link #setDataObject(DataType, Path, String, String)}</li>
 * <li>{@link #setDataObject(DataType, String, String, String, Long, OAISDataObjectLocationDto...)}</li>
 * <li>{@link #setDataObject(DataType, Path, String, String, String, Long)}</li>
 * <li>{@link #setDataObject(DataType, Path, String)}</li>
 * <li>{@link #setDataObject(DataType, Path, String, String)}</li>
 * </ul>
 * <br/>
 * To set the representation information, use :
 * <ul>
 * <li>{@link #setSyntax(String, String, MimeType)}</li>
 * <li>{@link #setSyntaxAndSemantic(String, String, MimeType, String)}</li>
 * <li>{@link #addHardwareEnvironmentProperty(String, Object)}</li>
 * <li>{@link #addSoftwareEnvironmentProperty(String, Object)}</li>
 * </ul>
 * <br/>
 *
 * @author Marc Sordi
 * @deprecated Use {@link InformationPackageProperties} fluent API
 */
public class ContentInformationDtoBuilder implements IOAISBuilder<ContentInformationDto> {

    private final ContentInformationDto ci = new ContentInformationDto();

    @Override
    public ContentInformationDto build() {
        return ci;
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
    public void setDataObject(DataType dataType,
                              String filename,
                              String algorithm,
                              String checksum,
                              Long fileSize,
                              OAISDataObjectLocationDto... locations) {
        Assert.notNull(dataType, "Data type is required");
        Assert.hasText(filename, "Filename is required");
        Assert.hasText(algorithm, "Checksum algorithm is required");
        Assert.hasText(checksum, "Checksum is required");
        Assert.notEmpty(locations, "At least one location is required");

        OAISDataObjectDto dataObject = new OAISDataObjectDto();
        dataObject.setFilename(filename);
        dataObject.setRegardsDataType(dataType);
        dataObject.setLocations(new HashSet<>(Arrays.asList(locations)));
        dataObject.setAlgorithm(algorithm);
        dataObject.setChecksum(checksum);
        dataObject.setFileSize(fileSize);
        ci.setDataObject(dataObject);
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
    public void setDataObject(DataType dataType,
                              Path filePath,
                              String filename,
                              String algorithm,
                              String checksum,
                              Long fileSize) {
        setDataObject(dataType, filename, algorithm, checksum, fileSize, OAISDataObjectLocationDto.build(filePath));
    }

    /**
     * Alias for {@link ContentInformationDtoBuilder#setDataObject(DataType, Path, String, String, String, Long)} (no
     * file size)
     *
     * @param dataType  {@link DataType}
     * @param filePath  reference to the physical file
     * @param algorithm checksum algorithm
     * @param checksum  the checksum
     */
    public void setDataObject(DataType dataType, Path filePath, String algorithm, String checksum) {
        setDataObject(dataType, filePath, filePath.getFileName().toString(), algorithm, checksum, null);
    }

    /**
     * Alias for {@link ContentInformationDtoBuilder#setDataObject(DataType, Path, String, String, String, Long)} (no file
     * size and MD5 default checksum algorithm)
     *
     * @param dataType {@link DataType}
     * @param filePath reference to the physical file
     * @param checksum the checksum
     */
    public void setDataObject(DataType dataType, Path filePath, String checksum) {
        setDataObject(dataType, filePath, filePath.getFileName().toString(), IPBuilder.MD5_ALGORITHM, checksum, null);
    }

    /**
     * Set syntax representation
     *
     * @param mimeName        MIME name
     * @param mimeDescription MIME description
     * @param mimeType        MIME type
     */
    public void setSyntax(String mimeName, String mimeDescription, MimeType mimeType) {
        Assert.notNull(mimeType, "Mime type cannot be null");
        Assert.hasLength(mimeType.getType(), "Mime type type cannot be null");
        Assert.hasLength(mimeType.getSubtype(), "Mime type subtype cannot be null");

        SyntaxDto syntax = new SyntaxDto();
        syntax.setName(mimeName);
        syntax.setDescription(mimeDescription);
        syntax.setMimeType(mimeType);

        ci.setRepresentationInformation(new RepresentationInformationDto());
        ci.getRepresentationInformation().setSyntax(syntax);
    }

    /**
     * Set syntax representation
     *
     * @param mimeType MIME type
     */
    public void setSyntax(MimeType mimeType) {
        setSyntax(null, null, mimeType);
    }

    /**
     * Set syntax and <b>optional</b> semantic representations
     *
     * @param mimeName            MIME name
     * @param mimeDescription     MIME description
     * @param mimeType            MIME type
     * @param semanticDescription semantic description
     */
    public void setSyntaxAndSemantic(String mimeName,
                                     String mimeDescription,
                                     MimeType mimeType,
                                     String semanticDescription) {
        setSyntax(mimeName, mimeDescription, mimeType);

        Assert.hasLength(semanticDescription, "Semantic description cannot be null. Use alternative method otherwise.");
        SemanticDto semantic = new SemanticDto();
        semantic.setDescription(semanticDescription);

        ci.getRepresentationInformation().setSemantic(semantic);
    }

    /**
     * Set syntax and <b>optional</b> semantic representations
     *
     * @param mimeType            MIME type
     * @param semanticDescription semantic description
     */
    public void setSyntaxAndSemantic(MimeType mimeType, String semanticDescription) {
        setSyntaxAndSemantic(null, null, mimeType, semanticDescription);
    }

    /**
     * Add sofware Environment property with the given parameters
     */
    public void addSoftwareEnvironmentProperty(String key, Object value) {
        Assert.hasLength(key, "Software environment information key is required");
        Assert.notNull(value, "Software environment information value is required");
        ci.getRepresentationInformation().getEnvironmentDescription().getSoftwareEnvironment().put(key, value);
    }

    /**
     * Add hardware environment property with the given parameters
     */
    public void addHardwareEnvironmentProperty(String key, Object value) {
        Assert.hasLength(key, "Hardware environment information key is required");
        Assert.notNull(value, "Hardware environment information value is required");
        ci.getRepresentationInformation().getEnvironmentDescription().getHardwareEnvironment().put(key, value);
    }

}
