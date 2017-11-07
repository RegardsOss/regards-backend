package fr.cnes.regards.modules.storage.service.database;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.modules.storage.domain.database.AIPDataBase;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IAIPEntityService {

    Page<AIPDataBase> retrieveBySip(String sipId, Pageable pageable);
}
