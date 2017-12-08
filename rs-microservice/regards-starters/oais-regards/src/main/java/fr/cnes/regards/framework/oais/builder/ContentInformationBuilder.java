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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.RepresentationInformation;
import fr.cnes.regards.framework.oais.Semantic;
import fr.cnes.regards.framework.oais.Syntax;
import fr.cnes.regards.framework.oais.urn.DataType;

/**
 *
 * Content Information Builder.<br/>
 *
 * A {@link ContentInformation} is composed of two objects :
 * <ul>
 * <li>An {@link OAISDataObject} containing physical file information</li>
 * <li>A {@link RepresentationInformation} object describing how to handle,understand,etc. this data object.</li>
 * </ul>
 * <hr>
 * This builder helps to fill in these objects.
 * <br/>
 * <br/>
 * To define the data object, use one of the following methods :
 * <ul>
 * <li>{@link #setDataObject(DataType, URL, String, String)}</li>
 * <li>{@link #setDataObject(DataType, Path, String, String)}</li>
 * <li>{@link #setDataObject(DataType, URL, String, String, String, Long)}</li>
 * <li>{@link #setDataObject(DataType, Path, String, String, String, Long)}</li>
 * <li>{@link #setDataObject(DataType, Path, String)}</li>
 * <li>{@link #setDataObject(DataType, URL, String)}</li>
 * <li>{@link #setDataObject(DataType, Path, String, String)}</li>
 * <li>{@link #setDataObject(DataType, URL, String, String)}</li>
 * <li>{@link #setDataObject(DataType, Path, String, String, Long)}</li>
 * <li>{@link #setDataObject(DataType, URL, String, String, Long)</li>
 * </ul>
 * <br/>
 * To set the representation information, use :
 * <ul>
 * <li>{@link #setSyntax(String, String, String)}</li>
 * <li>{@link #setSyntaxAndSemantic(String, String, String, String)}</li>
 * <li>{@link #addHardwareEnvironmentProperty(String, Object)}</li>
 * <li>{@link #addSoftwareEnvironmentProperty(String, Object)}</li>
 * </ul>
 * <br/>
 *
 * @author Marc Sordi
 *
 */
public class ContentInformationBuilder implements IOAISBuilder<ContentInformation> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentInformationBuilder.class);

    private final ContentInformation ci = new ContentInformation();

    @Override
    public ContentInformation build() {
        return ci;
    }

    /**
     * Set <b>required</b> data object reference and information
     * @param dataType {@link DataType}
     * @param url reference to the physical file
     * @param filename optional filename (may be null)
     * @param algorithm checksum algorithm
     * @param checksum the checksum
     * @param fileSize file size
     */
    public void setDataObject(DataType dataType, URL url, String filename, String algorithm, String checksum,
            Long fileSize) {
        Assert.notNull(dataType, "Data type is required");
        Assert.notNull(url, "URL is required");

        OAISDataObject dataObject = new OAISDataObject();
        dataObject.setFilename(filename);
        dataObject.setRegardsDataType(dataType);
        dataObject.setUrl(url);
        dataObject.setAlgorithm(algorithm);
        dataObject.setChecksum(checksum);
        dataObject.setFileSize(fileSize);
        ci.setDataObject(dataObject);
    }

    /**
     * Set <b>required</b> data object reference and information
     * @param dataType {@link DataType}
     * @param filePath reference to the physical file
     * @param filename optional filename (may be null)
     * @param algorithm checksum algorithm
     * @param checksum the checksum
     * @param fileSize file size
     */
    public void setDataObject(DataType dataType, Path filePath, String filename, String algorithm, String checksum,
            Long fileSize) {
        Assert.notNull(filePath, "Data path is required");
        try {
            setDataObject(dataType, filePath.toUri().toURL(), filename, algorithm, checksum, fileSize);
        } catch (MalformedURLException e) {
            String errorMessage = String.format("Cannot transform %s to valid URL (MalformedURLException).",
                                                filePath.toString());
            LOGGER.error(errorMessage, e);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Alias for {@link #setDataObject(DataType, URL, String, String, String, Long)} with MD5 default checksum algorithm
     * @param dataType {@link DataType}
     * @param url reference to the physical file
     * @param filename optional filename (may be null)
     * @param checksum the checksum
     * @param fileSize file size
     */
    public void setDataObject(DataType dataType, URL url, String filename, String checksum, Long fileSize) {
        setDataObject(dataType, url, filename, IPBuilder.MD5_ALGORITHM, checksum, fileSize);
    }

    /**
     * Alias for {@link #setDataObject(DataType, Path, String, String, String, Long)} with MD5 default checksum
     * algorithm
     * @param dataType {@link DataType}
     * @param filePath reference to the physical file
     * @param filename optional filename (may be null)
     * @param checksum the checksum
     * @param fileSize file size
     */
    public void setDataObject(DataType dataType, Path filePath, String filename, String checksum, Long fileSize) {
        setDataObject(dataType, filePath, filename, IPBuilder.MD5_ALGORITHM, checksum, fileSize);
    }

    /**
     * Alias for {@link ContentInformationBuilder#setDataObject(DataType, URL, String, String, String, Long)} (no
     * filename and no filesize)
     * @param dataType {@link DataType}
     * @param url reference to the physical file
     * @param algorithm checksum algorithm
     * @param checksum the checksum
     */
    public void setDataObject(DataType dataType, URL url, String algorithm, String checksum) {
        setDataObject(dataType, url, null, algorithm, checksum, null);
    }

    /**
     * Alias for {@link ContentInformationBuilder#setDataObject(DataType, Path, String, String, String, Long)} (no
     * filename and no filesize)
     * @param dataType {@link DataType}
     * @param filePath reference to the physical file
     * @param algorithm checksum algorithm
     * @param checksum the checksum
     */
    public void setDataObject(DataType dataType, Path filePath, String algorithm, String checksum) {
        setDataObject(dataType, filePath, filePath.getFileName().toString(), algorithm, checksum, null);
    }

    /**
     * Alias for {@link ContentInformationBuilder#setDataObject(DataType, URL, String, String, String, Long)} (no
     * filename, no filesize and MD5 default checksum algorithm)
     * @param dataType {@link DataType}
     * @param url reference to the physical file
     * @param checksum the checksum
     */
    public void setDataObject(DataType dataType, URL url, String checksum) {
        setDataObject(dataType, url, null, IPBuilder.MD5_ALGORITHM, checksum, null);
    }

    /**
     * Alias for {@link ContentInformationBuilder#setDataObject(DataType, Path, String, String, String, Long)} (no
     * filename, no filesize and MD5 default checksum algorithm)
     * @param dataType {@link DataType}
     * @param filePath reference to the physical file
     * @param checksum the checksum
     */
    public void setDataObject(DataType dataType, Path filePath, String checksum) {
        setDataObject(dataType, filePath, filePath.getFileName().toString(), IPBuilder.MD5_ALGORITHM, checksum, null);
    }

    /**
     * Set syntax representation
     * @param mimeName MIME name
     * @param mimeDescription MIME description
     * @param mimeType MIME type
     */
    public void setSyntax(String mimeName, String mimeDescription, String mimeType) {
        Assert.hasLength(mimeName, "Mime name cannot be null.");
        Assert.hasLength(mimeDescription, "Mime description cannot be null");
        Assert.hasLength(mimeType, "Mime type cannot be null");

        Syntax syntax = new Syntax();
        syntax.setName(mimeName);
        syntax.setDescription(mimeDescription);
        syntax.setMimeType(mimeType);

        ci.setRepresentationInformation(new RepresentationInformation());
        ci.getRepresentationInformation().setSyntax(syntax);
    }

    /**
     * Set syntax and <b>optional</b> semantic representations
     * @param mimeName MIME name
     * @param mimeDescription MIME description
     * @param mimeType MIME type
     * @param semanticDescription semantic description
     */
    public void setSyntaxAndSemantic(String mimeName, String mimeDescription, String mimeType,
            String semanticDescription) {
        setSyntax(mimeName, mimeDescription, mimeType);

        Assert.hasLength(semanticDescription, "Semantic description cannot be null. Use alternative method otherwise.");
        Semantic semantic = new Semantic();
        semantic.setDescription(semanticDescription);

        ci.getRepresentationInformation().setSemantic(semantic);
    }

    /**
     * Add sofware Environment property with the given parameters
     * @param key
     * @param value
     */
    public void addSoftwareEnvironmentProperty(String key, Object value) {
        Assert.hasLength(key, "Software environment information key is required");
        Assert.notNull(value, "Software environment information value is required");
        ci.getRepresentationInformation().getEnvironmentDescription().getSoftwareEnvironment().put(key, value);
    }

    /**
     * Add hardware environment property with the given parameters
     * @param key
     * @param value
     */
    public void addHardwareEnvironmentProperty(String key, Object value) {
        Assert.hasLength(key, "Hardware environment information key is required");
        Assert.notNull(value, "Hardware environment information value is required");
        ci.getRepresentationInformation().getEnvironmentDescription().getHardwareEnvironment().put(key, value);
    }

}
