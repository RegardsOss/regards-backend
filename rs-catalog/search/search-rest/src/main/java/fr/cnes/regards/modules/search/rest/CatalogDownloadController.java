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
package fr.cnes.regards.modules.search.rest;

import java.io.IOException;
import java.util.Collection;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import feign.Response;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;

/**
 * REST Controller handling operations on downloads.
 *
 * @author Kevin Marchois
 */
@RestController
@RequestMapping(CatalogDownloadController.PATH_DOWNLOAD)
public class CatalogDownloadController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogDownloadController.class);

    public static final String PATH_DOWNLOAD = "/downloads";

    public static final String DOWNLOAD_AIP_FILE = "/{aip_id}/files/{checksum}";

    /**
     * AIP ID path parameter
     */
    public static final String AIP_ID_PATH_PARAM = "aip_id";

    /**
     * checksum path parameter
     */
    public static final String CHECKSUM_PATH_PARAM = "checksum";

    @Autowired
    private IStorageRestClient storageRestClient;

    /**
     * Business search service
     */
    @Autowired
    protected ICatalogSearchService searchService;

    /**
     * Download a file that user has right to
     * @param aipId aip id where is the file
     * @param checksum checksum on the file
     * @return the file to download
     * @throws ModuleException
     * @throws IOException
     */
    @RequestMapping(path = DOWNLOAD_AIP_FILE, method = RequestMethod.GET, produces = MediaType.ALL_VALUE)
    @ResourceAccess(description = "download one file from a given AIP by checksum.", role = DefaultRole.PUBLIC)
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable(AIP_ID_PATH_PARAM) String aipId,
            @PathVariable(CHECKSUM_PATH_PARAM) String checksum) throws ModuleException, IOException {
        OaisUniformResourceName urn = OaisUniformResourceName.fromString(aipId);
        if (this.searchService.hasAccess(urn)) {
            FeignSecurityManager.asSystem();
            try {
                Response response = storageRestClient.downloadFile(checksum);
                InputStreamResource isr = null;
                if (response.status() == HttpStatus.OK.value()) {
                    isr = new InputStreamResource(response.body().asInputStream());
                } else {
                    LOGGER.error("Error downloading file {} from storage", checksum);
                }
                HttpHeaders headers = new HttpHeaders();
                for (Entry<String, Collection<String>> h : response.headers().entrySet()) {
                    h.getValue().forEach(v -> headers.add(h.getKey(), v));
                }
                return new ResponseEntity<>(isr, headers, HttpStatus.valueOf(response.status()));
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                LOGGER.error(String.format("Error downloading file through storage microservice. Cause : %s",
                                           e.getMessage()),
                             e);
                return new ResponseEntity<InputStreamResource>(HttpStatus.INTERNAL_SERVER_ERROR);
            } finally {
                FeignSecurityManager.reset();
            }
        }
        return new ResponseEntity<InputStreamResource>(HttpStatus.FORBIDDEN);

    }
}
