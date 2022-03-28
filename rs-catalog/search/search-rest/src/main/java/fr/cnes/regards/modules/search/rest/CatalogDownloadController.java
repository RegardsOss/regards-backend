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
package fr.cnes.regards.modules.search.rest;

import feign.Response;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.ResponseStreamProxy;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.autoconfigure.CustomCacheControlHeadersWriter;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.accessrights.domain.projects.LicenseDTO;
import fr.cnes.regards.modules.dam.client.entities.IAttachmentClient;
import fr.cnes.regards.modules.search.domain.download.Download;
import fr.cnes.regards.modules.search.domain.download.FailedDownload;
import fr.cnes.regards.modules.search.domain.download.MissingLicenseDownload;
import fr.cnes.regards.modules.search.domain.download.ValidDownload;
import fr.cnes.regards.modules.search.rest.download.LicenseAccessor;
import fr.cnes.regards.modules.search.service.ICatalogSearchService;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.springframework.http.MediaType.ALL_VALUE;

/**
 * REST Controller handling operations on downloads.
 *
 * <b>Note : </b> See {@link CustomCacheControlHeadersWriter} to know more about cache control handling for download resources.
 *
 * @author Kevin Marchois
 */
@RestController
@RequestMapping(path = CatalogDownloadController.PATH_DOWNLOAD)
public class CatalogDownloadController {

    public static final String PATH_DOWNLOAD = "/downloads";

    private static final String DOWNLOAD_AIP_FILE = "/{aip_id}/files/{checksum}";

    private static final String DOWNLOAD_DAM_FILE = DOWNLOAD_AIP_FILE + "/dam";

    /**
     * AIP ID path parameter
     */
    private static final String AIP_ID_PATH_PARAM = "aip_id";

    /**
     * checksum path parameter
     */
    private static final String CHECKSUM_PATH_PARAM = "checksum";

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogDownloadController.class);

    @Autowired
    private ICatalogSearchService searchService;

    @Autowired
    private IStorageRestClient storageRestClient;

    @Autowired
    private IAttachmentClient attachmentClient;

    @Autowired
    private LicenseAccessor licenseAccessor;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @RequestMapping(path = DOWNLOAD_DAM_FILE, method = RequestMethod.GET, produces = ALL_VALUE)
    @ResourceAccess(description = "Proxy download for dam locally stored files", role = DefaultRole.PUBLIC)
    public ResponseEntity<InputStreamResource> downloadDamFile(@PathVariable(AIP_ID_PATH_PARAM) String aipId,
                                                               @PathVariable(CHECKSUM_PATH_PARAM) String checksum,
                                                               @RequestParam(name = "origin", required = false)
                                                                   String origin,
                                                               @RequestParam(name = "isContentInline", required = false)
                                                                   Boolean isContentInline,
                                                               HttpServletResponse response) throws IOException {
        FeignSecurityManager.asSystem();
        try {
            Response damResp = attachmentClient.getFile(aipId, checksum, origin, isContentInline);
            if (damResp.status() != HttpStatus.OK.value()) {
                LOGGER.error("Error downloading file {} from storage", checksum);
            }
            addHeaders(damResp, response);
            return formatDamResponse(damResp);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            LOGGER.error(String.format("Error downloading file through storage microservice. Cause : %s",
                                       e.getMessage()), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    private ResponseEntity<InputStreamResource> formatDamResponse(Response fromDamResponse) throws IOException {
        return ResponseEntity.status(fromDamResponse.status())
            .headers(new HttpHeaders())
            .body(computeDamBody(fromDamResponse));
    }

    private InputStreamResource computeDamBody(Response fromDamResponse) throws IOException {
        // Even if dam response is an error,
        // the body should be added in Response body
        return fromDamResponse.body() != null ?
            new InputStreamResource(new ResponseStreamProxy(fromDamResponse)) :
            null;
    }

    /**
     * Endpoint that enables to verify product access before download files of this product.
     * It verifies the user privileges and the license acceptation.
     *
     * @param productUrn product identifier
     * @return empty response containing only a status that indicates product access state.
     */
    @RequestMapping(path = DOWNLOAD_AIP_FILE, method = RequestMethod.HEAD, produces = ALL_VALUE)
    @ResourceAccess(description = "test product access and license acceptation before download.",
                    role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> testProductAccess(String productUrn) {
        HttpStatus status = HttpStatus.OK;
        try {
            if (!isUserAuthorized(productUrn)) {
                status = HttpStatus.FORBIDDEN;
            }
            if (isLicenseUnaccepted()) {
                status = HttpStatus.LOCKED;
            }
        } catch (EntityNotFoundException e) {
            status = HttpStatus.NOT_FOUND;
        } catch (ExecutionException e) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(status).build();
    }

    /**
     * Endpoint to download a file from external acces (gui or api).
     * It checks user privileges and licence acceptation
     * before downloading the file.
     *
     * @param aipId           product id to download
     * @param checksum        checksum of the file to download
     * @param isContentInline isContentInline when parameter value is true, server disable a browser security that disallow file content display inside the webapp
     * @param acceptLicense   indicate if the user want to automatically accept the license
     * @param response        http response, used to set the response headers
     * @return response body and status
     * @throws ModuleException if an error occurred during the licence verification or the file download
     * @throws IOException     if the downloaded file is not streamable
     */
    @RequestMapping(path = DOWNLOAD_AIP_FILE, method = RequestMethod.GET, produces = ALL_VALUE)
    @ResourceAccess(description = "download one file from a given AIP by checksum.", role = DefaultRole.PUBLIC)
    public ResponseEntity<Download> downloadFile(@PathVariable(AIP_ID_PATH_PARAM) String aipId,
                                                 @PathVariable(CHECKSUM_PATH_PARAM) String checksum,
                                                 @RequestParam(name = "isContentInline", required = false)
                                                     Boolean isContentInline,
                                                 @RequestParam(name = "acceptLicense",
                                                               required = false,
                                                               defaultValue = "false") Boolean acceptLicense,
                                                 HttpServletResponse response) throws ModuleException, IOException {
        if (!isUserAuthorized(aipId)) {
            return unauthorizedAccess();
        }
        try {
            if (acceptLicense) {
                acceptLicense();
            }
            if (isLicenseUnaccepted()) {
                return acceptLicenceBeforeDownload(linkToLicense(),
                                                   linkToDownloadWithLicense(aipId,
                                                                             checksum,
                                                                             isContentInline,
                                                                             response));
            }
            // To download through storage client we must be authenticated as user in order to
            // impact the download quotas, but we upgrade the privileges so that the request passes.
            FeignSecurityManager.asUser(authResolver.getUser(), DefaultRole.PROJECT_ADMIN.name());
            Response storageResponse = downloadFile(checksum, isContentInline);
            addHeaders(storageResponse, response);
            if (storageResponse.status() == HttpStatus.OK.value()) {
                return downloadedFile(storageResponse);
            }
            return failedDownload(storageResponse);
        } catch (HttpClientErrorException | HttpServerErrorException | ExecutionException e) {
            return internalError(checksum, e);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    private boolean isUserAuthorized(String productId) throws EntityNotFoundException {
        return searchService.hasAccess(UniformResourceName.fromString(productId));
    }

    private void acceptLicense() throws ModuleException {
        licenseAccessor.acceptLicense(authResolver.getUser(), runtimeTenantResolver.getTenant());
    }

    private LicenseDTO retrieveLicense() throws ExecutionException {
        return licenseAccessor.retrieveLicense(authResolver.getUser(), runtimeTenantResolver.getTenant());
    }

    private boolean isLicenseUnaccepted() throws ExecutionException {
        return !retrieveLicense().isAccepted();
    }

    private String linkToLicense() throws ExecutionException {
        return retrieveLicense().getLicenceLink();
    }

    private Link linkToDownloadWithLicense(String aipId,
                                           String checksum,
                                           Boolean isContentInline,
                                           HttpServletResponse response) throws ModuleException, IOException {
        return linkTo(methodOn(CatalogDownloadController.class).downloadFile(aipId,
                                                                             checksum,
                                                                             isContentInline,
                                                                             true,
                                                                             response)).withRel("accept");
    }

    private Response downloadFile(String checksum, Boolean isContentInline) {
        Response storageResponse = storageRestClient.downloadFile(checksum, isContentInline);
        if (storageResponse.status() != HttpStatus.OK.value()) {
            LOGGER.error("Error downloading file {} from storage", checksum);
        }
        return storageResponse;
    }

    private void addHeaders(Response fromStorageResponse, HttpServletResponse inResponse) {
        // Add storage headers in the response
        // CacheControl headers are filtered because This download endpoints must not activate cache control.
        // Headers are not added in ResponseEntity but in HttpServletResponse
        // because headers are not correctly handled in ResponseEntity
        // in integration with Feign.
        fromStorageResponse.headers()
            .entrySet()
            .stream()
            .filter(header -> !CustomCacheControlHeadersWriter.isCacheControlHeader(header.getKey()))
            .forEach(header -> header.getValue().forEach(value -> inResponse.setHeader(header.getKey(), value)));
    }

    private ResponseEntity<Download> unauthorizedAccess() {
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<Download> acceptLicenceBeforeDownload(String linkToLicence, Link linkToAcceptAndDownload) {
        MissingLicenseDownload body = new MissingLicenseDownload(linkToLicence,
                                                                 linkToAcceptAndDownload.toUri().toString());
        return ResponseEntity.status(HttpStatus.LOCKED).body(body);
    }

    private ResponseEntity<Download> downloadedFile(Response fromStorageResponse) throws IOException {
        return ResponseEntity.status(fromStorageResponse.status())
            .body(new ValidDownload(new ResponseStreamProxy(fromStorageResponse)));
    }

    private ResponseEntity<Download> failedDownload(Response fromStorageResponse) throws IOException {
        FailedDownload body = fromStorageResponse.body() != null ?
            new FailedDownload(new ResponseStreamProxy(fromStorageResponse)) :
            null;
        return ResponseEntity.status(fromStorageResponse.status()).body(body);
    }

    private ResponseEntity<Download> internalError(String checksum, Exception e) {
        String downloadFailed = String.format(
            "Error while downloading file %s with licence and access verification. Cause : %s",
            checksum,
            e.getMessage());
        LOGGER.error(downloadFailed, e);
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
