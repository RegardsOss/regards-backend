package fr.cnes.regards.modules.order.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.common.io.ByteStreams;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.TooManyResultsException;
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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwt;

/**
 * @author oroussel
 */
@Controller
@RequestMapping("")
public class OrderDataFileController implements IResourceController<OrderDataFile> {

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
    @RequestMapping(method = RequestMethod.GET, path = "/orders/{orderId}/dataset/{datasetId}/files")
    public ResponseEntity<List<Resource<OrderDataFile>>> findFiles(@PathVariable("orderId") Long orderId,
            @PathVariable("datasetId") Long datasetId) throws TooManyResultsException {
        DatasetTask dsTask = datasetTaskService.loadSimple(datasetId);
        if (dsTask.getFilesCount() > maximumDisplayableDataFiles) {
            throw new TooManyResultsException(String.format("Too many files (> to %d files)", dsTask.getFilesCount()));
        }
        dsTask = datasetTaskService.loadComplete(datasetId);
        List<Resource<OrderDataFile>> dataFiles = new ArrayList<>();
        for (FilesTask filesTask : dsTask.getReliantTasks()) {
            for (OrderDataFile dataFile : filesTask.getFiles()) {
                Resource<OrderDataFile> resource = toResource(dataFile);
                resourceService.addLink(resource, this.getClass(), "downloadFile", "download",
                                        MethodParamFactory.build(Long.class, orderId),
                                        MethodParamFactory.build(Long.class, datasetId),
                                        MethodParamFactory.build(String.class, dataFile.getIpId().toString()),
                                        MethodParamFactory.build(String.class, dataFile.getChecksum()));
                dataFiles.add(resource);
            }
        }
        return ResponseEntity.ok(dataFiles);
    }

    @ResourceAccess(description = "Download a file that is part of an order", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET, path = "/orders/{orderId}/aips/{aipId}/files/{checksum}")
    public void downloadFile(@PathVariable("orderId") Long orderId, @PathVariable("aipId") String aipId,
            @PathVariable("checksum") String checksum, HttpServletResponse response)
            throws NoSuchElementException, IOException {
        dataFileService.downloadFile(orderId, UniformResourceName.fromString(aipId), checksum, response);
    }

    @ResourceAccess(description = "Download a file that is part of an order granted by token",
            role = DefaultRole.PUBLIC)
    @RequestMapping(method = RequestMethod.GET, path = "/orders/aips/{aipId}/files/{checksum}")
    public void publicDownloadFile(@PathVariable("aipId") String aipId, @PathVariable("checksum") String checksum,
            @RequestParam(name = "token") String token, HttpServletResponse response)
            throws NoSuchElementException, IOException, InvalidJwtException {
        // Throws an invalidJwtException if secret isn't correct or expiration date has been reached
        Claims claims = jwtService.parseToken(token, secret);
        Long orderId = claims.get(OrderController.ORDER_ID_KEY, Long.class);
        dataFileService.downloadFile(orderId, UniformResourceName.fromString(aipId), checksum, response);
    }

    @Override
    public Resource<OrderDataFile> toResource(OrderDataFile dataFile, Object... pExtras) {
        return resourceService.toResource(dataFile);
    }
}
