/*
 * Copyright 2017-20XX CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest.download;

import feign.Response;
import fr.cnes.regards.modules.search.domain.download.Download;
import fr.cnes.regards.modules.search.domain.download.FailedDownload;
import fr.cnes.regards.modules.search.domain.download.MissingLicenseDownload;
import fr.cnes.regards.modules.search.domain.download.ValidDownload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

/**
 * @author Thomas Fache
 **/
public class CatalogDownloadResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogDownloadResponse.class);

    private CatalogDownloadResponse() {
    }

    public static ResponseEntity<Download> unauthorizedAccess() {
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    public static ResponseEntity<Download> acceptLicenceBeforeDownload(String linkToLicence,
                                                                       String linkToAcceptAndDownload) {
        MissingLicenseDownload body = new MissingLicenseDownload(linkToLicence, linkToAcceptAndDownload);
        return ResponseEntity.status(HttpStatus.LOCKED).contentType(MediaType.APPLICATION_JSON).body(body);
    }

    public static ResponseEntity<Download> successfulDownload(Response fromStorageResponse) throws IOException {
        return ResponseEntity.status(fromStorageResponse.status()).body(new ValidDownload(fromStorageResponse));
    }

    public static ResponseEntity<Download> failedDownload(Response fromStorageResponse) throws IOException {
        FailedDownload body = fromStorageResponse.body() != null ? new FailedDownload(fromStorageResponse) : null;
        return ResponseEntity.status(fromStorageResponse.status()).body(body);
    }

    public static ResponseEntity<Download> internalError(String checksum, Exception e) {
        String downloadFailed = String.format("Error while downloading file %s. Cause : %s", checksum, e.getMessage());
        LOGGER.error(downloadFailed, e);
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
