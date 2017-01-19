/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.DataObject;
import fr.cnes.regards.modules.storage.urn.UniformResourceName;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public interface IAIPService {

    /**
     * retrieve pages of AIP filtered according to the parameters
     *
     * @param pTo
     *            maximum date of last event that affected any AIP wanted
     * @param pFrom
     *            minimum date of submission into REGARDS wanted
     * @param pState
     *            State of AIP wanted
     * @param pPageable
     *            pageable object
     * @return filtered page of AIP
     */
    Page<AIP> retrieveAIPs(AIPState pState, LocalDateTime pFrom, LocalDateTime pTo, Pageable pPageable);

    /**
     * @param pAIP
     * @return
     */
    AIP create(AIP pAIP);

    /**
     * @param pIpId
     * @return
     * @throws EntityNotFoundException
     */
    List<DataObject> retrieveAIPFiles(UniformResourceName pIpId) throws EntityNotFoundException;

    /**
     * @param pIpId
     * @return
     */
    List<UniformResourceName> retrieveAIPVersionHistory(UniformResourceName pIpId);

}
