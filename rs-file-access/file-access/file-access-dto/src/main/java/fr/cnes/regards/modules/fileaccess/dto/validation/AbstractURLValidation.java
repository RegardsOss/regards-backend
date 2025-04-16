/*

 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.fileaccess.dto.validation;

/**
 * Abstract class for url validators
 *
 * @author Sébastien Binda
 **/
public abstract class AbstractURLValidation {

    protected boolean isValidUrl(String urlToValidate) {
        if (urlToValidate == null || urlToValidate.isBlank()) {
            return false;
        }
        // NOTE:
        // A valid URL for the storage microservice cannot contain an anchor (# character) or parameters (? character),
        // with the exception of parameters (? character).
        // Since REGARDS handles files in archives or cut files for long-term archives, some URLs may contain
        // the 'fileName' parameter or 'parts' parameter. For more details, refer to the rs-file-packager microservice
        // for archiving small files or the CNES-specific plugin for STAF V3.

        // Explanation of the regex:
        // The URL must start with a protocol (http, https, file)
        // The URL must not contain an anchor (# character)
        // The URL must not contain parameters (? character) unless they are 'fileName' or 'parts'
        return urlToValidate.matches("^([a-zA-Z][a-zA-Z\\d+\\-.]*:\\/\\/|file:\\/)(?!.*#)(?!.*\\?(?!file|parts)).*$");
    }

}
