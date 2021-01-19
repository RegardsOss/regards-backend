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
package fr.cnes.regards.modules.order.rest;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.service.IDatasetTaskService;
import fr.cnes.regards.modules.order.service.IOrderDataFileService;
import fr.cnes.regards.modules.order.service.IOrderService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.NoSuchElementException;

/**
 * @author oroussel
 */
@RestController
@RequestMapping("")
public class OrderDataFileController implements IResourceController<OrderDataFile> {

    public static final String ORDERS_AIPS_AIP_ID_FILES_ID = "/orders/aips/{aipId}/files/{dataFileId}";

    public static final String ORDERS_FILES_DATA_FILE_ID = "/orders/files/{dataFileId}";

    public static final String ORDERS_ORDER_ID_DATASET_DATASET_ID_FILES = "/orders/{orderId}/dataset/{datasetId}/files";

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IDatasetTaskService datasetTaskService;

    @Autowired
    private IOrderDataFileService dataFileService;

    @Autowired
    private JWTService jwtService;

    @Value("${regards.order.secret}")
    private String secret;

    @ResourceAccess(description = "Find all files from order for specified dataset", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = ORDERS_ORDER_ID_DATASET_DATASET_ID_FILES)
    public ResponseEntity<PagedModel<EntityModel<OrderDataFile>>> findFiles(@PathVariable("orderId") Long orderId,
            @PathVariable("datasetId") Long datasetId, Pageable pageRequest,
            PagedResourcesAssembler<OrderDataFile> assembler) {
        Page<OrderDataFile> dataFiles = datasetTaskService.loadDataFiles(datasetId, pageRequest);
        return ResponseEntity.ok(toPagedResources(dataFiles, assembler));
    }

    @ResourceAccess(description = "Download a file that is part of an order", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = ORDERS_FILES_DATA_FILE_ID)
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable("dataFileId") Long dataFileId,
            HttpServletResponse response) throws NoSuchElementException {
        // Throws a NoSuchElementException if not found
        OrderDataFile dataFile = dataFileService.load(dataFileId);
        // External files haven't necessarily a file name (but they have an URL)
        String filename = dataFile.getFilename() != null ? dataFile.getFilename()
                : dataFile.getUrl().substring(dataFile.getUrl().lastIndexOf('/') + 1);
        response.addHeader("Content-disposition", "attachment;filename=" + filename);
        if (dataFile.getMimeType() != null) {
            response.setContentType(dataFile.getMimeType().toString());
        }

        return new ResponseEntity<>(os -> dataFileService.downloadFile(dataFile, os), HttpStatus.OK);
    }

    @ResourceAccess(description = "Test file download availability", role = DefaultRole.PUBLIC)
    @RequestMapping(method = RequestMethod.HEAD, path = ORDERS_AIPS_AIP_ID_FILES_ID)
    public ResponseEntity<StreamingResponseBody> testDownloadFile(@PathVariable("aipId") String aipId,
            @PathVariable("dataFileId") Long dataFileId, @RequestParam(name = IOrderService.ORDER_TOKEN) String token,
            HttpServletResponse response) throws NoSuchElementException {
        return manageFile(Boolean.TRUE, dataFileId, token, response);
    }

    @ResourceAccess(description = "Download a file that is part of an order granted by token",
            role = DefaultRole.PUBLIC)
    @RequestMapping(method = RequestMethod.GET, path = ORDERS_AIPS_AIP_ID_FILES_ID)
    public ResponseEntity<StreamingResponseBody> publicDownloadFile(@PathVariable("aipId") String aipId,
            @PathVariable("dataFileId") Long dataFileId, @RequestParam(name = IOrderService.ORDER_TOKEN) String token,
            HttpServletResponse response) throws NoSuchElementException {
        return manageFile(Boolean.FALSE, dataFileId, token, response);
    }

    /**
     * Above controller endpoints are duplicated to fit security single endpoint policy.
     * (Otherwise, we could have set 2 HTTP method in a single endpoint!)
     */
    private ResponseEntity<StreamingResponseBody> manageFile(Boolean headRequest, Long dataFileId, String token,
            HttpServletResponse response) throws NoSuchElementException {
        OrderDataFile dataFile;
        String user;
        String role;
        try {
            Claims claims = jwtService.parseToken(token, secret);
            Long.parseLong(claims.get(IOrderService.ORDER_ID_KEY, String.class));
            user = claims.get(JWTService.CLAIM_SUBJECT).toString();
            role = claims.get(JWTService.CLAIM_ROLE).toString();
            // Throws a NoSuchElementException if not found
            dataFile = dataFileService.load(dataFileId);
        } catch (JwtException | InvalidJwtException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        switch (dataFile.getState()) {
            case PENDING:
                // For JDownloader : status 202 when file not yet available
                return new ResponseEntity<>(HttpStatus.ACCEPTED);
            case ERROR:
                // Error from storage
                return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
            default:
                if (headRequest) {
                    // Omit payload, just send an OK response
                    return new ResponseEntity<>(HttpStatus.OK);
                } else {
                    String filename = dataFile.getFilename() != null ? dataFile.getFilename()
                            : dataFile.getUrl().substring(dataFile.getUrl().lastIndexOf('/') + 1);
                    response.addHeader("Content-disposition", "attachment;filename=" + filename);
                    if (dataFile.getMimeType() != null) {
                        response.setContentType(dataFile.getMimeType().toString());
                    }
                    // Stream the response
                    return new ResponseEntity<>(os -> {
                        FeignSecurityManager.asUser(user, role);
                        try {
                            dataFileService.downloadFile(dataFile, os);
                        } finally {
                            FeignSecurityManager.reset();
                        }
                    }, HttpStatus.OK);
                }
        }
    }

    @Override
    public EntityModel<OrderDataFile> toResource(OrderDataFile dataFile, Object... extras) {
        EntityModel<OrderDataFile> resource = resourceService.toResource(dataFile);
        resourceService.addLink(resource, this.getClass(), "downloadFile", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, dataFile.getId()),
                                MethodParamFactory.build(HttpServletResponse.class));
        return resource;
    }
}
