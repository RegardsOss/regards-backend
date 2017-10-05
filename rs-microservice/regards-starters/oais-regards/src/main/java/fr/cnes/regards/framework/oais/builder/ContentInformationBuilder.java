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

import org.springframework.util.Assert;

import fr.cnes.regards.framework.oais.*;
import fr.cnes.regards.framework.oais.urn.DataType;

/**
 *
 * Content Information Builder
 *
 * @author Marc Sordi
 *
 */
public class ContentInformationBuilder implements IOAISBuilder<ContentInformation> {

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
     */
    public void setDataObject(DataType dataType, URL url, @Nullable String filename) {
        Assert.notNull(dataType, "Data type is required");
        Assert.notNull(url, "URL is required");

        OAISDataObject dataObject = new OAISDataObject();
        dataObject.setFilename(filename);
        dataObject.setRegardsDataType(dataType);
        dataObject.setUrl(url);
        ci.setDataObject(dataObject);
    }

    /**
     * Alias for {@link ContentInformationBuilder#setDataObject(DataType, URL, String)} (no filename)
     * @param dataType {@link DataType}
     * @param url reference to the physical file
     */
    public void setDataObject(DataType dataType, URL url) {
        setDataObject(dataType, url, null);
    }

    /**
     * Set <b>optional</b> syntax representation
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
     * Set <b>optional</b> syntax and semantic representations
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

    public void addSoftwareEnvironment(String key, Object value) {
        Assert.hasLength(key, "Software environment information key is required");
        Assert.notNull(value, "Software environment information value is required");
        ci.getRepresentationInformation().getEnvironmentDescription().getSoftwareEnvironment().put(key, value);
    }

    public void addHardwareEnvironment(String key, Object value) {
        Assert.hasLength(key, "Hardware environment information key is required");
        Assert.notNull(value, "Hardware environment information value is required");
        ci.getRepresentationInformation().getEnvironmentDescription().getHardwareEnvironment().put(key, value);
    }

}
