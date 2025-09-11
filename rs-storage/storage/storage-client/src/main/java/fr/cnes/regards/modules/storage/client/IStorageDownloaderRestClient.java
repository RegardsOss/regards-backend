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
package fr.cnes.regards.modules.storage.client;

import feign.Response;
import fr.cnes.regards.framework.feign.annotation.RestClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST Client to access storage microservice
 *
 * @author Sébastien Binda
 */
@RestClient(name = "rs-downloader", contextId = "rs-downloader.rest.client")
public interface IStorageDownloaderRestClient extends IStorageDownloadQuotaRestClient {

    String FILE_PATH = "/files";

    String DOWNLOAD_PATH = "/{checksum}/download";

    /**
     * Download a file by its checksum.
     *
     * @param checksum file to download
     */
    @RequestMapping(method = RequestMethod.GET,
                    path = FILE_PATH + DOWNLOAD_PATH,
                    produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    Response downloadFile(@PathVariable("checksum") String checksum,
                          @RequestParam(name = "isContentInline", required = false, defaultValue = "false")
                          boolean isContentInline);

}
