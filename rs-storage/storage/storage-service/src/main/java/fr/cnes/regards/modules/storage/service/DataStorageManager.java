/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.storage.domain.AIP;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@Service
public class DataStorageManager {

    // TODO
    @Autowired
    private IAllocationStrategy allocationStrategy;

    public void storeAip(AIP pAip) {
        // get All data storage impl
        // get Storage target for the description file from AllocationStrategy
        // execute the storage of descriptor
        // schedule the upload of files referenced by the AIP
    }

}
