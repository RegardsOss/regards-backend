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
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.order.service.IDatasetTaskService;
import fr.cnes.regards.modules.order.service.IOrderDataFileService;

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

    @Value("${regards.order.files.displayable.maximum:5000}")
    private int maximumDisplayableDataFiles;

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

    @ResourceAccess(description = "Download file", role = DefaultRole.REGISTERED_USER)
    @RequestMapping(method = RequestMethod.GET,
            path = "/orders/{orderId}/dataset/{datasetId}/aips/{aipId}/files/{checksum}")
    public void downloadFile(@PathVariable("orderId") Long orderId, @PathVariable("datasetId") Long datasetId,
            @PathVariable("aipId") String aipId, @PathVariable("checksum") String checksum,
            HttpServletResponse response) throws NoSuchElementException, IOException {
        dataFileService.downloadFile(orderId, UniformResourceName.fromString(aipId), checksum, response);
    }

    @Override
    public Resource<OrderDataFile> toResource(OrderDataFile dataFile, Object... pExtras) {
        return resourceService.toResource(dataFile);
    }
}
