/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import feign.Response;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.ResponseStreamProxy;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.autoconfigure.CustomCacheControlHeadersWriter;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;

/**
 * REST Controller handling operations on downloads.
 *
 * <b>Note : </b> See {@link CustomCacheControlHeadersWriter} to know more about cache control handling for download resources.
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

    @Autowired
    private IAuthenticationResolver authResolver;

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
            @PathVariable(CHECKSUM_PATH_PARAM) String checksum,
            @RequestParam(name="isContentInline", required=false) Boolean isContentInline) throws ModuleException, IOException {
        UniformResourceName urn = UniformResourceName.fromString(aipId);
        if (this.searchService.hasAccess(urn)) {
            // To download through storage client we must be authenticate as user in order to
            // impact the download quotas, but we upgrade the privileges so that the request passes.
            FeignSecurityManager.asUser(authResolver.getUser(), DefaultRole.PROJECT_ADMIN.name());
            Response response = null;
            try {
                response = storageRestClient.downloadFile(checksum, isContentInline);
                InputStreamResource isr = null;
                HttpHeaders headers = new HttpHeaders();
                // Add all headers from storage microservice response except for cache control ones.
                // This download endpoints must not activate cache control. Cache control is handled by CustomCacheControlHeaderWriter
                for (Entry<String, Collection<String>> h : response.headers().entrySet()) {
                    if ((!h.getKey().equalsIgnoreCase(CustomCacheControlHeadersWriter.CACHE_CONTROL))
                            && (!h.getKey().equalsIgnoreCase(CustomCacheControlHeadersWriter.EXPIRES))
                            && (!h.getKey().equalsIgnoreCase(CustomCacheControlHeadersWriter.PRAGMA))) {
                        h.getValue().forEach(v -> headers.add(h.getKey(), v));
                    }
                }
                if (response.status() == HttpStatus.OK.value()) {
                    isr = new InputStreamResource(new ResponseStreamProxy(response));
                } else {
                    LOGGER.error("Error downloading file {} from storage", checksum);
                    // if body is not null, forward the error content too
                    if (response.body() != null) {
                        isr = new InputStreamResource(new ResponseStreamProxy(response));
                    }
                }
                return ResponseEntity.status(response.status()).headers(headers).body(isr);
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
