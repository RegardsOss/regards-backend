/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;
import fr.cnes.regards.modules.storage.plugin.datastorage.IDataStorage;
import fr.cnes.regards.modules.storage.service.job.UpdateDataFilesJob;
import fr.cnes.regards.modules.storage.service.scheduler.UpdateMetadataScheduler;

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
     * <li>Storage in db of {@link AIP}</li>
     * <li>Storage in db of each {@link DataFile} associated</li>
     * <li>Physical storage of each {@link DataFile} through {@link IDataStorage} plugins</li>
     * <li>Creation of physical file containing AIP metadata informations and storage through {@link IDataStorage}
     * plugins</li>
     * </ul>
     * @param pAIP new {@link Set}<{@link AIP}> to store
     * @return {@link Set}<{@link UUID}> of scheduled store AIP Jobs.
     * @throws ModuleException
     */
    Set<UUID> store(Set<AIP> pAIP) throws ModuleException;

    /**
     * Make asked files available into the cache file system if necessary.<br/>
     * Files that are already available are return in the response. For other ones, asynchronous jobs are scheduled
     * to make them available. As soon as a file is available, a {@link DataFileEvent} is published into the
     * system message queue.
     * @param {@link AvailabilityRequest} containing request informations. Files checksum to make available and
     *            files lifetime in cache.
     * @return checksums of files that are already available
     */
    AvailabilityResponse loadFiles(AvailabilityRequest availabilityRequest) throws ModuleException;

    /**
     * Retrieve pages of AIP filtered according to the parameters
     *
     * @param pState {@link AIPState} State of AIP wanted
     * @param pFrom {@link OffsetDateTime} start date of AIP to retrieve
     * @param pTo {@link OffsetDateTime} stop date of AIP to retrieve
     * @param pPageable {@link Pageable} Pagination information
     * @return {@link AIP}s corresponding to parameters given.
     */
    Page<AIP> retrieveAIPs(AIPState pState, OffsetDateTime pFrom, OffsetDateTime pTo, Pageable pPageable)
            throws ModuleException;

    /**
     * @param pIpId
     * @return
     * @throws EntityNotFoundException
     */
    Set<OAISDataObject> retrieveAIPFiles(UniformResourceName pIpId) throws ModuleException;

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

    /**
     * Retrieve the local {@link Path} of the {@link DataFile} associated to the given {@link AIP} and matching the
     * given checksum.<br/>
     * Return Optional.empty if the {@link DataFile} is not accesible localy.<br/>
     * @param pAipId
     * @param pChecksum
     * @return
     * @throw EntityNotFoundException if the request {@link DataFile} does not exists.
     */
    Optional<DataFile> getAIPDataFile(String pAipId, String pChecksum) throws ModuleException;

    List<Event> retrieveAIPHistory(UniformResourceName pIpId) throws ModuleException;

    Set<AIP> retrieveAipsBulk(Set<String> ipIds);

    Set<AIP> retrieveAipsByTag(String tag);

    AIP retrieveAip(String ipId) throws EntityNotFoundException;

    /**
     * Update PDI and descriptive information of an aip according to updated. To add/remove ContentInformation, store a
     * new aip with a different version and use store method.
     * @param ipId information package identifier of the aip
     * @param updated object containing changes
     * @return aip stored into the system after changes have been propagated
     * @throws EntityNotFoundException if no aip with ipId as identifier can be found
     * @throws EntityInconsistentIdentifierException if ipId and updated ipId are different
     * @throws EntityOperationForbiddenException if aip in the system is not in the right state
     */
    AIP updateAip(String ipId, AIP updated)
            throws EntityNotFoundException, EntityInconsistentIdentifierException, EntityOperationForbiddenException;

    /**
     * Remove an aip from the system. Its file are deleted if and only if no other aip point to them.
     */
    Set<UUID> deleteAip(String ipId) throws ModuleException;

    ///////////////////////////////////////////////////////////////////////////////////////////
    ////////////////// These methods should only be called by IAIPServices
    ///////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Schedule new {@link UpdateDataFilesJob}s for all {@link DataFile} of AIP metadata files given
     * and set there state to STORING_METADATA.
     * @param metadataToStore List of {@link DataFile} of new AIP metadata files mapped to old ones.
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
