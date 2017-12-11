/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import java.nio.file.Path;
import java.time.OffsetDateTime;
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
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.AipDataFiles;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.RejectedAip;
import fr.cnes.regards.modules.storage.domain.database.DataFile;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.service.job.UpdateDataFilesJob;

/**
 * Service Interface to handle {@link AIP} entities.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Sébastien Binda
 */
public interface IAIPService {

    /**
     * Schedule asynchronous jobs to handle new {@link AIP}s creation.<br/>
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
    Set<UUID> storeAndCreate(Set<AIP> pAIP) throws ModuleException;

    /**
     * Schedule asynchronous jobs to handle failed storage of existing {@link AIP}.<br/>
     * @param aipIpIds collection of aip ip ids to try to store back
     */
    Set<UUID> storeRetry(Set<String> aipIpIds) throws ModuleException;

    /**
     * Apply creation validation on each {@link AIP}:
     * <ul>
     *     <li>Aip does not already exists in the database</li>
     *     <li>Aip is valid</li>
     * </ul>
     * @param aips
     * @return Rejected aips, threw their ip id, with the rejection cause.
     */
    List<RejectedAip> applyCreationChecks(AIPCollection aips);

    /**
     * Apply retry validation on each {@link AIP} represented by their ipId:
     * <ul>
     *     <li>Aip is known in the system</li>
     * </ul>
     * @param aipIpIds
     * @return
     */
    List<RejectedAip> applyRetryChecks(Set<String> aipIpIds);

    /**
     * Make asked files available into the cache file system if necessary.<br/>
     * Files that are already available are return in the response. For other ones, asynchronous jobs are scheduled
     * to make them available. As soon as a file is available, a {@link DataFileEvent} is published into the
     * system message queue.
     * @param availabilityRequest {@link AvailabilityRequest} containing request informations. Files checksum to make available and
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
     * Retrieve pages of AIP with files public information filtered according to the parameters
     *
     * @param state
     * @param tags
     * @param fromLastUpdateDate
     * @param pageable
     * @return {@link AIP}s corresponding to parameters given.
     */
    Page<AipDataFiles> retrieveAipDataFiles(AIPState state, Set<String> tags, OffsetDateTime fromLastUpdateDate, Pageable pageable);

    /**
     * Retrieve the files metadata associated to an aip
     * @param pIpId
     * @return the files metadata
     * @throws EntityNotFoundException
     */
    Set<OAISDataObject> retrieveAIPFiles(UniformResourceName pIpId) throws ModuleException;

    /**
     * Retrieve the versions of an aip
     * @param pIpId
     * @return the aip versions ip ids
     */
    List<String> retrieveAIPVersionHistory(UniformResourceName pIpId);

    /**
     * Handle update of physical AIP metadata files associated to {@link AIP} updated in database.
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

    /**
     * Retrieve the history of event that occurred to an aip, represented by its ip id
     * @param pIpId
     * @return the aip history
     * @throws ModuleException
     */
    List<Event> retrieveAIPHistory(UniformResourceName pIpId) throws ModuleException;

    /**
     * Retrieve all aips which ip id is at least one of the provided ones
     * @param ipIds
     * @return all aips which ip id is at least one of the provided ones
     */
    Set<AIP> retrieveAipsBulk(Set<String> ipIds);

    /**
     * Retrieve all aips that are tagged by the given tag
     * @param tag
     * @return tagged aips
     */
    Set<AIP> retrieveAipsByTag(String tag);

    /**
     * Retrieve an aip thanks to its ip id
     * @param ipId
     * @return the aip
     * @throws EntityNotFoundException
     */
    AIP retrieveAip(String ipId) throws EntityNotFoundException;

    /**
     * Update PDI and descriptive information of an aip according to updated. To add/remove ContentInformation, storeAndCreate a
     * new aip with a different version and use storeAndCreate method.
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

    /**
     * Remove {@link AIP}s associated the given sip, threw its ip id
     * @param sipIpId
     */
    void deleteAipFromSip(String sipIpId) throws ModuleException;

    /**
     * Add tags to the specified aip, threw its ip id
     * @param ipId
     * @param tagsToAdd
     */
    void addTags(String ipId, Set<String> tagsToAdd)
            throws EntityNotFoundException, EntityInconsistentIdentifierException, EntityOperationForbiddenException;

    /**
     * Removes tads from a specified aip, threw its ip id
     * @param ipId
     * @param tagsToRemove
     */
    void removeTags(String ipId, Set<String> tagsToRemove) throws EntityNotFoundException,
            EntityInconsistentIdentifierException, EntityOperationForbiddenException;

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
     * Schedule the storage of metadata update
     * @param metadataToUpdate
     */
    void scheduleStorageMetadataUpdate(Set<UpdatableMetadataFile> metadataToUpdate);

    /**
     * Prepare the aip metadata of already stored aip that has been updated
     * @return the new and old aip metadata associated data file
     */
    Set<UpdatableMetadataFile> prepareUpdatedAIP();

    /**
     * Prepare the aip metadata that are not yet stored
     * @return data files to store
     */
    Set<DataFile> prepareNotFullyStored();
}
