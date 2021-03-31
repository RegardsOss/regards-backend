package fr.cnes.regards.modules.toponyms.service;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.modules.toponyms.dao.ToponymsRepository;
import fr.cnes.regards.modules.toponyms.domain.Toponym;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RegardsTransactional
public class TemporaryToponymsCleanService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemporaryToponymsCleanService.class);

    @Autowired
    private ToponymsRepository toponymsRepository;

    @Value("${regards.toponyms.limit.temporary:30}")
    private int limitTemporary;

    /**
     * Delete all out-dated not visible toponyms
     */
    public int clean() {
        OffsetDateTime currentDateTime = OffsetDateTime.now();
        int nbDeleted = 0;
        LOGGER.debug("Deleting expired files from cache. Current date : {}", currentDateTime.toString());
        Pageable page = PageRequest.of(0, 100);
        Page<Toponym> toponymsToDelete;
        do {
            toponymsToDelete = toponymsRepository.findByVisibleAndToponymMetadataExpirationDateBefore(false, OffsetDateTime.now(), page);
            this.toponymsRepository.deleteInBatch(toponymsToDelete);
            nbDeleted += toponymsToDelete.getNumberOfElements();
        } while (toponymsToDelete.hasNext());
        return nbDeleted;
    }
}

