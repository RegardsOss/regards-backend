/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.dam.service.entities;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.dam.service.entities.exception.InvalidContentTypeException;
import fr.cnes.regards.modules.dam.service.entities.exception.InvalidFilenameException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Utility class to validate supported content type for a file data type.
 *
 * @author Marc Sordi
 */
public final class ContentTypeValidator {

    /**
     * Before the RFC7763 (March 2016), some browser were using a different MediaType.
     */
    public final static String TEXT_MARKDOWN_ALTERNATIVE_MEDIATYPE = "text/x-markdown";

    /**
     * List of file content types allowed on a product description
     */
    public static final List<String> CONTENT_TYPE_DESCRIPTION_ALLOWED = Arrays.asList(MediaType.APPLICATION_PDF_VALUE,
                                                                                      MediaType.TEXT_MARKDOWN_VALUE,
                                                                                      TEXT_MARKDOWN_ALTERNATIVE_MEDIATYPE,
                                                                                      MediaType.TEXT_HTML_VALUE,
                                                                                      MediaType.TEXT_PLAIN_VALUE);

    private ContentTypeValidator() {
        // Nothing to do
    }

    public static void supports(DataType dataType, String filename, String contentType) throws ModuleException {
        // Original file must be given
        if (StringUtils.isEmpty(filename)) {
            throw new InvalidFilenameException();
        }
        // Restrictions only applies to DESCRIPTION for now
        if (DataType.DESCRIPTION.equals(dataType)) {
            checkFileSupported(contentType, CONTENT_TYPE_DESCRIPTION_ALLOWED);
        }
    }

    private static void checkFileSupported(String contentType, Collection<String> expectedContentTypes)
        throws InvalidContentTypeException {

        // Check content types
        if (expectedContentTypes != null) {
            String shortContentType = getContentType(contentType);
            if (!expectedContentTypes.contains(shortContentType)) {
                throw new InvalidContentTypeException(expectedContentTypes, shortContentType);
            }
        }
    }

    private static String getContentType(String contentType) {
        if (contentType != null) {
            int charsetIdx = contentType.indexOf(";charset");
            return (charsetIdx == -1) ? contentType.trim() : contentType.substring(0, charsetIdx).trim();
        }
        return null;
    }
}
