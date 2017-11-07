package fr.cnes.regards.modules.storage.service.database;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.modules.storage.dao.IAIPDataBaseRepository;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Service
@RegardsTransactional
public class AIPEntityService implements IAIPEntityService {

    @Autowired
    private IAIPDataBaseRepository aipDataBaseRepository;

    @Override
    public Page<AIPEntity> retrieveBySip(String sipId, Pageable pageable) {
        return aipDataBaseRepository.findAllBySipId(sipId, pageable);
    }
}
