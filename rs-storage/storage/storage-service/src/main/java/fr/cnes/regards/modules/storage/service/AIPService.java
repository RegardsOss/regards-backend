/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.storage.dao.IAIPRepository;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.DataObject;
import fr.cnes.regards.modules.storage.urn.UniformResourceName;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class AIPService implements IAIPService {

    private final IAIPRepository repo;

    /**
     *
     */
    public AIPService(IAIPRepository pRepo) {
        repo = pRepo;
    }

    /**
     * AIP has been validated by Controller REST. Validation of an AIP is only to check if network has not corrupted
     * informations.(There is another validation point when each file is stocked as file are only downloaded by
     * asynchronous task)
     */
    @Override
    public AIP create(AIP pAIP) {
        // TODO Auto-generated method stub
        return repo.save(pAIP);
    }

    @Override
    public Page<AIP> retrieveAIPs(AIPState pState, LocalDateTime pFrom, LocalDateTime pTo, Pageable pPageable) { // NOSONAR
        if (pState != null) {
            if (pFrom != null) {
                if (pTo != null) {
                    return repo.findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(pState, pFrom.minusNanos(1),
                                                                                           pTo.minusNanos(1),
                                                                                           pPageable);
                }
                return repo.findAllByStateAndSubmissionDateAfter(pState, pFrom.minusNanos(1), pPageable);
            }
            if (pTo != null) {
                return repo.findAllByStateAndLastEventDateBefore(pState, pTo.minusNanos(1), pPageable);
            }
            return repo.findAllByState(pState, pPageable);
        }
        if (pFrom != null) {
            if (pTo != null) {
                return repo.findAllBySubmissionDateAfterAndLastEventDateBefore(pFrom.minusNanos(1), pTo.minusNanos(1),
                                                                               pPageable);
            }
            return repo.findAllBySubmissionDateAfter(pFrom.minusNanos(1), pPageable);
        }
        if (pTo != null) {
            return repo.findAllByLastEventDateBefore(pTo, pPageable);
        }
        return repo.findAll(pPageable);
    }

    @Override
    public List<DataObject> retrieveAIPFiles(UniformResourceName pIpId) throws EntityNotFoundException {
        AIP aip = repo.findOneByIpIdWithDataObjects(pIpId.toString());
        if (aip == null) {
            throw new EntityNotFoundException(pIpId.toString(), AIP.class);
        }
        return aip.getDataObjects();
    }

    @Override
    public List<String> retrieveAIPVersionHistory(UniformResourceName pIpId) {
        String ipIdWithoutVersion = pIpId.toString();
        ipIdWithoutVersion = ipIdWithoutVersion.substring(0, ipIdWithoutVersion.indexOf(":V"));
        List<AIP> versions = repo.findAllByIpIdStartingWith(ipIdWithoutVersion);
        return versions.stream().map(a -> a.getIpId()).collect(Collectors.toList());
    }

}
