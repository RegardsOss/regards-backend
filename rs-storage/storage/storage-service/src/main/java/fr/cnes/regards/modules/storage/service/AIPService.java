/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.storage.dao.IAIPRepository;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.DataObject;
import fr.cnes.regards.modules.storage.domain.event.AIPValid;
import fr.cnes.regards.modules.storage.urn.UniformResourceName;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Service
public class AIPService implements IAIPService {

    private final IAIPRepository repo;

    private final IPublisher publisher;

    private final DataStorageManager storageManager;

    public AIPService(IAIPRepository pRepo, IPublisher pPublisher, DataStorageManager pStorageManager) {
        repo = pRepo;
        publisher = pPublisher;
        storageManager = pStorageManager;
    }

    /**
     * AIP has been validated by Controller REST. Validation of an AIP is only to check if network has not corrupted
     * informations.(There is another validation point when each file is stocked as file are only downloaded by
     * asynchronous task)
     *
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @Override
    public Long create(AIP pAIP) throws NoSuchAlgorithmException, IOException {
        // save into DB as valid
        AIP aip = new AIP(pAIP);
        aip.setState(AIPState.VALID);
        aip = repo.save(pAIP);

        // stockage du descriptif sur fichier
        // TODO: job synchrone?
        // ouverture du flux sur le Workspace
        // ecriture sur workspace
        // récupération du stockage pérenne avec AllocationStrategy
        // move sur le pérenne

        // Publish AIP_VALID
        publisher.publish(new AIPValid(aip));

        // scheduleJob for files just give to the job the AIP ipId or id
        Long jobId = null;
        // TODO: schedule store of files
        // change the state to PENDING
        pAIP.setState(AIPState.PENDING);

        return jobId;
    }

    /**
     * two {@link LocalDateTime} are here considered equals to the second
     */
    @Override
    public Page<AIP> retrieveAIPs(AIPState pState, LocalDateTime pFrom, LocalDateTime pTo, Pageable pPageable) { // NOSONAR
        if (pState != null) {
            if (pFrom != null) {
                if (pTo != null) {
                    return repo.findAllByStateAndSubmissionDateAfterAndLastEventDateBefore(pState, pFrom.minusNanos(1),
                                                                                           pTo.plusSeconds(1),
                                                                                           pPageable);
                }
                return repo.findAllByStateAndSubmissionDateAfter(pState, pFrom.minusNanos(1), pPageable);
            }
            if (pTo != null) {
                return repo.findAllByStateAndLastEventDateBefore(pState, pTo.plusSeconds(1), pPageable);
            }
            return repo.findAllByState(pState, pPageable);
        }
        if (pFrom != null) {
            if (pTo != null) {
                return repo.findAllBySubmissionDateAfterAndLastEventDateBefore(pFrom.minusNanos(1), pTo.plusSeconds(1),
                                                                               pPageable);
            }
            return repo.findAllBySubmissionDateAfter(pFrom.minusNanos(1), pPageable);
        }
        if (pTo != null) {
            return repo.findAllByLastEventDateBefore(pTo.plusSeconds(1), pPageable);
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
