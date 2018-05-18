package fr.cnes.regards.modules.storage.service.database;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.modules.storage.domain.database.AIPEntity;

/**
 * Contract to respect by classes which wants to handle AIPEntities
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public interface IAIPEntityService {

    /**
     * Retrieve a page of aips which sip id is the provided one
     * @param sipId
     * @param pageable
     * @return a page of aips
     */
    Page<AIPEntity> retrieveBySip(String sipId, Pageable pageable);
}
