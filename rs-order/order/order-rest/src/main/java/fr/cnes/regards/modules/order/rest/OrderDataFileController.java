package fr.cnes.regards.modules.order.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.util.UriUtils;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Chars;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.TooManyResultsException;
import fr.cnes.regards.framework.module.rest.utils.HttpUtils;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.InvalidJwtException;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.service.IDatasetTaskService;
import fr.cnes.regards.modules.order.service.IOrderDataFileService;
import fr.cnes.regards.modules.order.service.IOrderService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.MalformedJwtException;

/**
 * @author oroussel
 */
@RestController
@RequestMapping("")
public class OrderDataFileController implements IResourceController<OrderDataFile> {

    public static final String ORDERS_AIPS_AIP_ID_FILES_CHECKSUM = "/orders/aips/{aipId}/files/{checksum}";

    public static final String ORDERS_ORDER_ID_AIPS_AIP_ID_FILES_CHECKSUM = "/orders/{orderId}/aips/{aipId}/files/{checksum}";

    public static final String ORDERS_ORDER_ID_DATASET_DATASET_ID_FILES = "/orders/{orderId}/dataset/{datasetId}/files";

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IDatasetTaskService datasetTaskService;

    @Autowired
    private IOrderDataFileService dataFileService;

    @Autowired
    private JWTService jwtService;

    @Value("${regards.order.files.displayable.maximum:5000}")
    private int maximumDisplayableDataFiles;

    @Value("${regards.order.secret}")
    private String secret;

    @ResourceAccess(description = "Find all files from order for specified dataset", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = ORDERS_ORDER_ID_DATASET_DATASET_ID_FILES)
    public ResponseEntity<Page<Resource<OrderDataFile>>> findFiles(@PathVariable("orderId") Long orderId,
            @PathVariable("datasetId") Long datasetId, Pageable pageRequest) {
        DatasetTask dsTask = datasetTaskService.loadComplete(datasetId);
        int cpt = 0;
        List<Resource<OrderDataFile>> dataFiles = new ArrayList<>();
        for (FilesTask filesTask : dsTask.getReliantTasks()) {
            for (OrderDataFile dataFile : filesTask.getFiles()) {
                if ((cpt >= pageRequest.getOffset()) && (cpt < pageRequest.getOffset() + pageRequest.getPageSize())) {
                    Resource<OrderDataFile> resource = toResource(dataFile);
                    resourceService.addLink(resource, this.getClass(), "downloadFile", "download",
                                            MethodParamFactory.build(Long.class, orderId),
                                            MethodParamFactory.build(Long.class, datasetId),
                                            MethodParamFactory.build(String.class, dataFile.getIpId().toString()),
                                            MethodParamFactory.build(String.class, dataFile.getChecksum()));
                    dataFiles.add(resource);
                } else if (cpt >= pageRequest.getOffset() + pageRequest.getPageSize()) {
                    return ResponseEntity.ok(new PageImpl<>(dataFiles, pageRequest, dataFiles.size()));
                }
                cpt++;
            }
        }
        return ResponseEntity.ok(new PageImpl<>(dataFiles, pageRequest, dataFiles.size()));
    }

    @ResourceAccess(description = "Download a file that is part of an order", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = ORDERS_ORDER_ID_AIPS_AIP_ID_FILES_CHECKSUM)
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable("orderId") Long orderId,
            @PathVariable("aipId") String aipId, @PathVariable("checksum") String checksum,
            HttpServletResponse response) throws NoSuchElementException, IOException {
        // Throws a NoSuchELementException if not found
        OrderDataFile dataFile = dataFileService.find(orderId, decodeUrn(aipId), checksum);
        response.addHeader("Content-disposition", "attachment;filename=" + dataFile.getName());
        response.setContentType(dataFile.getMimeType().toString());

        return new ResponseEntity<>(os -> dataFileService.downloadFile(dataFile, decodeUrn(aipId), checksum, os),
                                    HttpStatus.OK);
    }

    @ResourceAccess(description = "Download a file that is part of an order granted by token",
            role = DefaultRole.PUBLIC)
    @RequestMapping(method = RequestMethod.GET, path = ORDERS_AIPS_AIP_ID_FILES_CHECKSUM)
    public ResponseEntity<StreamingResponseBody> publicDownloadFile(@PathVariable("aipId") String aipId,
            @PathVariable("checksum") String checksum, @RequestParam(name = IOrderService.ORDER_TOKEN) String token,
            HttpServletResponse response) throws NoSuchElementException, IOException {
        OrderDataFile dataFile;
        try {
            Claims claims = jwtService.parseToken(token, secret);
            Long orderId = Long.parseLong(claims.get(IOrderService.ORDER_ID_KEY, String.class));
            // Throws a NoSuchElementException if not found
            dataFile = dataFileService.find(orderId, decodeUrn(aipId), checksum);
        } catch (InvalidJwtException | MalformedJwtException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        response.addHeader("Content-disposition", "attachment;filename=" + dataFile.getName());
        response.setContentType(dataFile.getMimeType().toString());

        switch (dataFile.getState()) {
            case PENDING:
                // For JDownloader : status 202 when file not yet available
                return new ResponseEntity<>(HttpStatus.ACCEPTED);
            case ERROR:
                // Error from storage
                return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
            default:
                // Stream the response
                return new ResponseEntity<>(
                        os -> dataFileService.downloadFile(dataFile, decodeUrn(aipId), checksum, os), HttpStatus.OK);
        }
    }

    private static UniformResourceName decodeUrn(String aipId) throws UnsupportedEncodingException {
        return UniformResourceName.fromString(UriUtils.decode(aipId, Charset.defaultCharset().name()));
    }

    @Override
    public Resource<OrderDataFile> toResource(OrderDataFile dataFile, Object... pExtras) {
        return resourceService.toResource(dataFile);
    }
}
