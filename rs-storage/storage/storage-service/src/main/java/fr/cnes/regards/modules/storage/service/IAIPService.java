/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.DataObject;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.database.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;
import fr.cnes.regards.modules.storage.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.service.job.UpdateDataFilesJob;

/**
 * Service Interface to handle {@link AIP} entities.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Sébastien Binda
 */
public interface IAIPService {

    /**
     * Run asynchronous jobs to handle new {@link AIP}s creation.<br/>
     * This process handle :
     * <ul>
     * <li> Storage in db of {@link AIP}</li>
     * <li> Storage in db of each {@link DataFile} associated </li>
     * <li> Physical storage of each {@link DataFile} through {@link IDataStorage} plugins</li>
     * <li> Creation of physical file containing AIP metadata informations and storage through {@link IDataStorage} plugins</li>
     * </ul>
     * @param pAIP new {@link Set}<{@link AIP}> to create
     * @return {@link Set}<{@link UUID}> of scheduled store AIP Jobs.
     * @throws ModuleException
     */
    Set<UUID> create(Set<AIP> pAIP) throws ModuleException;

    /**
     * Make asked files available into the cache file system if necessary.<br/>
     * Files that are already available are return in the response. For other ones, asynchronous jobs are scheduled
     * to make them available. As soon as a file is available, a {@link DataFileEvent} is published into the
     * system message queue.
     * @param {@link AvailabilityRequest} containing request informations. Files checksum to make available and
     * files lifetime in cache.
     * @return checksums of files that are already available
     */
    AvailabilityResponse loadFiles(AvailabilityRequest availabilityRequest);

    /**
     * Retrieve pages of AIP filtered according to the parameters
     *
     * @param pState {@link AIPState} State of AIP wanted
     * @param pFrom {@link OffsetDateTime} start date of AIP to retrieve
     * @param pto {@link OffsetDateTime} stop date of AIP to retrieve
     * @param pPageable {@link Pageable} Pagination information
     * @return {@link AIP}s corresponding to parameters given.
     */
    Page<AIP> retrieveAIPs(AIPState pState, OffsetDateTime pFrom, OffsetDateTime pTo, Pageable pPageable);

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
    List<String> retrieveAIPVersionHistory(UniformResourceName pIpId);

    /**
     * Handle update of physical AIP metadata files associated to {@link AIP} updated in database.
     * This method is periodicly called by {@link UpdateMetadataScheduler}.
     */
    void updateAlreadyStoredMetadata();

    ///////////////////////////////////////////////////////////////////////////////////////////
    ////////////////// These methods should only be called by IAIPServices
    ///////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Schedule new {@link UpdateDataFilesJob}s for all {@link DataFile} of AIP metadata files given
     * and set there state to STORING_METADATA.
     * @param metadataToUpdate List of {@link DataFile} of new AIP metadata files mapped to old ones.
     */
    void scheduleStorageMetadata(Set<DataFile> metadataToStore);

    /**
     * Schedule
     * @param metadataToUpdate
     */
    void scheduleStorageMetadataUpdate(Set<UpdatableMetadataFile> metadataToUpdate);

    Set<UpdatableMetadataFile> prepareUpdatedAIP(Path tenantWorkspace);

    Set<DataFile> prepareNotFullyStored(Path tenantWorkspace);
}
