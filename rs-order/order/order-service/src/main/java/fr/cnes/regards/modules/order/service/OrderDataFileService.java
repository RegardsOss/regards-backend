package fr.cnes.regards.modules.order.service;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.common.io.ByteStreams;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.order.dao.IDatasetTaskRepository;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
import fr.cnes.regards.modules.order.domain.DatasetTask;
import fr.cnes.regards.modules.order.domain.FileState;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import fr.cnes.regards.modules.storage.client.IAipClient;

/**
 * @author oroussel
 */
@Service
@MultitenantTransactional
public class OrderDataFileService implements IOrderDataFileService {
    @Autowired
    private IOrderDataFileRepository repos;

    @Autowired
    private IAipClient aipClient;

    @Override
    public OrderDataFile save(OrderDataFile dataFile) {
        return repos.save(dataFile);
    }

    @Override
    public StreamingResponseBody downloadFile(Long orderId, UniformResourceName aipId, String checksum,
            HttpServletResponse response) {
        // Maybe this file is into an AIP that is asked several time for this Order (several Dataset for example)
        /// but we just need first one because its name is unique by AIP ID and checksum
        Optional<OrderDataFile> dataFileOpt = repos.findFirstByChecksumAndIpIdAndOrderId(checksum, aipId, orderId);
        if (!dataFileOpt.isPresent()) {
            throw new NoSuchElementException();
        }
        OrderDataFile dataFile = dataFileOpt.get();
        response.addHeader("Content-disposition", "attachment;filename=" + dataFile.getName());
        response.setContentType(dataFile.getMimeType().toString());

        return os -> {
            // Reading / Writing
            try (InputStream is = aipClient.downloadFile(aipId.toString(), checksum)) {
                ByteStreams.copy(is, os);
                os.flush();
                os.close();
            }
            repos.setStateForAipChecksumAndOrderId(FileState.DOWNLOADED, checksum, aipId, orderId);
        };
    }
}
