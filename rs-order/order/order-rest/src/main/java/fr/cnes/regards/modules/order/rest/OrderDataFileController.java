/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.NoSuchElementException;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderControllerEndpointConfiguration;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.service.IDatasetTaskService;
import fr.cnes.regards.modules.order.service.IOrderDataFileService;
import fr.cnes.regards.modules.order.service.IOrderService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;

/**
 * @author oroussel
 */
@RestController
public class OrderDataFileController implements IResourceController<OrderDataFile> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderDataFileController.class);

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IDatasetTaskService datasetTaskService;

    @Autowired
    private IOrderDataFileService dataFileService;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private JWTService jwtService;

    @Value("${regards.order.secret}")
    private String secret;

    @ResourceAccess(description = "Find all files from order for specified dataset", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET,
            path = OrderControllerEndpointConfiguration.ORDERS_ORDER_ID_DATASET_DATASET_ID_FILES)
    public ResponseEntity<PagedModel<EntityModel<OrderDataFile>>> findFiles(@PathVariable("orderId") Long orderId,
            @PathVariable("datasetId") Long datasetId, Pageable pageRequest,
            PagedResourcesAssembler<OrderDataFile> assembler) {
        Page<OrderDataFile> dataFiles = datasetTaskService.loadDataFiles(datasetId, pageRequest);
        return ResponseEntity.ok(toPagedResources(dataFiles, assembler));
    }

    @ResourceAccess(description = "Download a file that is part of an order", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = OrderControllerEndpointConfiguration.ORDERS_FILES_DATA_FILE_ID,
            produces = MediaType.ALL_VALUE)
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("dataFileId") Long dataFileId,
            HttpServletResponse response) throws NoSuchElementException {
        return manageFile(Boolean.FALSE, dataFileId, Optional.empty(), response);
    }

    @ResourceAccess(description = "Test file download availability", role = DefaultRole.PUBLIC)
    @RequestMapping(method = RequestMethod.HEAD,
            path = OrderControllerEndpointConfiguration.PUBLIC_ORDERS_FILES_DATA_FILE_ID)
    public ResponseEntity<InputStreamResource> testDownloadFile(@PathVariable("dataFileId") Long dataFileId,
            @RequestParam(name = IOrderService.ORDER_TOKEN) String token, HttpServletResponse response)
            throws NoSuchElementException {
        return manageFile(Boolean.TRUE, dataFileId, Optional.ofNullable(token), response);
    }

    @ResourceAccess(description = "Download a file that is part of an order granted by token",
            role = DefaultRole.PUBLIC)
    @RequestMapping(method = RequestMethod.GET,
            path = OrderControllerEndpointConfiguration.PUBLIC_ORDERS_FILES_DATA_FILE_ID,
            produces = MediaType.ALL_VALUE)
    public ResponseEntity<InputStreamResource> publicDownloadFile(@PathVariable("dataFileId") Long dataFileId,
            @RequestParam(name = IOrderService.ORDER_TOKEN, required = true) String token, HttpServletResponse response)
            throws NoSuchElementException {
        return manageFile(Boolean.FALSE, dataFileId, Optional.of(token), response);
    }

    /**
     * Above controller endpoints are duplicated to fit security single endpoint policy.
     * (Otherwise, we could have set 2 HTTP method in a single endpoint!)
     */
    private ResponseEntity<InputStreamResource> manageFile(Boolean headRequest, Long dataFileId,
            Optional<String> validityToken, HttpServletResponse response) throws NoSuchElementException {
        OrderDataFile dataFile;
        String user = null;
        if (validityToken.isPresent()) {
            try {
                Jwts.parser().setSigningKey(Encoders.BASE64.encode(secret.getBytes())).parse(validityToken.get());
                Claims claims = jwtService.parseToken(validityToken.get(), secret);
                Long.parseLong(claims.get(IOrderService.ORDER_ID_KEY, String.class));
                user = claims.get(JWTService.CLAIM_SUBJECT).toString();
            } catch (JwtException | InvalidJwtException e) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        }

        final Optional<String> asUser = Optional.ofNullable(user);

        // Throws a NoSuchElementException if not found
        dataFile = dataFileService.load(dataFileId);

        // Check if order owner of the file is the authentified user
        Order order = orderService.getOrder(dataFile.getOrderId());
        if (!order.getOwner().equals(asUser.orElse(authResolver.getUser()))) {
            LOGGER.error("Ordered file does not belongs to current user {}", asUser.orElse(authResolver.getUser()));
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
                    return new ResponseEntity<>(HttpStatus.OK);
                } else {
                    return dataFileService.downloadFile(dataFile, asUser);
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
