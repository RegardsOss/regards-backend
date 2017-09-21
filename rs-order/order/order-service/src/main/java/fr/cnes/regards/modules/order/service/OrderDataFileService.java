package fr.cnes.regards.modules.order.service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.io.ByteStreams;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.order.dao.IOrderDataFileRepository;
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

    @Autowired
    private IOrderDataFileService self;

    @Override
    public OrderDataFile save(OrderDataFile dataFile) {
        return repos.save(dataFile);
    }

    @Override
    public Iterable<OrderDataFile> save(Iterable<OrderDataFile> dataFiles) {
        return repos.save(dataFiles);
    }

    @Override
    public List<OrderDataFile> findAllAvailables(Long orderId) {
        return repos.findAllAvailables(orderId);
    }

    @Override
    public void downloadFile(Long orderId, UniformResourceName aipId, String checksum, HttpServletResponse response)
            throws IOException {
        // Maybe this file is into an AIP that is asked several time for this Order (several Dataset for example)
        /// but we just need first one because its name is unique by AIP ID and checksum
        Optional<OrderDataFile> dataFileOpt = repos.findFirstByChecksumAndIpIdAndOrderId(checksum, aipId, orderId);
        if (!dataFileOpt.isPresent()) {
            throw new NoSuchElementException();
        }
        OrderDataFile dataFile = dataFileOpt.get();
        response.addHeader("Content-disposition", "attachment;filename=" + dataFile.getName());
        response.setContentType(dataFile.getMimeType().toString());

        // Reading / Writing
        OutputStream os = response.getOutputStream();
        try (InputStream is = aipClient.downloadFile(aipId.toString(), checksum)) {
            ByteStreams.copy(is, os);
            os.close();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        // Update OrderDataFile (set State as DOWNLOADED, even if it is online)
        try {
            dataFile.setState(FileState.DOWNLOADED);
            self.save(dataFile);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
