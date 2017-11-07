package fr.cnes.regards.modules.storage.service.database;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.modules.storage.dao.IAIPDataBaseRepository;
import fr.cnes.regards.modules.storage.domain.database.AIPDataBase;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class AIPEntityService implements IAIPEntityService {

    @Autowired
    private IAIPDataBaseRepository aipDataBaseRepository;

    @Override
    public Page<AIPDataBase> retrieveBySip(String sipId, Pageable pageable) {
        return aipDataBaseRepository.findAllBySipId(sipId, pageable);
    }
}
