/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.storage.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;

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
import fr.cnes.regards.modules.storage.domain.RejectedSip;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.event.DataFileEvent;
import fr.cnes.regards.modules.storage.domain.job.AIPQueryFilters;
import fr.cnes.regards.modules.storage.domain.job.AddAIPTagsFilters;
import fr.cnes.regards.modules.storage.domain.job.RemoveAIPTagsFilters;
import fr.cnes.regards.modules.storage.domain.plugin.IAllocationStrategy;
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
     * Save AIP and publish event if requested
     */
    AIP save(AIP aip, boolean publish);

    /**
     * Synchronous method for validating and storing an AIP collection submitted through Rest API.<br/>
     * All heavy work will be done asynchronously.
     */
    List<RejectedAip> validateAndStore(AIPCollection aips) throws ModuleException;

    /**
     * Asynchronously makes the heavy work of storing AIP following these steps :
     * <ul>
     * <li>Extract data files from {@link AIP}</li>
     * <li>Dispatch them on {@link IDataStorage} plugins through the single active {@link IAllocationStrategy}
     * plugin</li>
     * <li>Prepare and schedule storage jobs for data files</li>
     * </ul>
     */
    void store() throws ModuleException;

    /**
     * Asynchronusly makes the heavy work of storing AIP metadata.
     */
    void storeMetadata();

    /**
     * Schedule asynchronous jobs to handle failed storage of existing {@link AIP}.<br/>
     * @param aipIpIds collection of aip ip ids to try to store back
     */
    void storeRetry(Set<String> aipIpIds) throws ModuleException;

    /**
     * Apply retry validation on each {@link AIP} represented by their ipId:
     * <ul>
     * <li>Aip is known in the system</li>
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
     * @param availabilityRequest {@link AvailabilityRequest} containing request informations. Files checksum to make
     *            available and
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
     * @param tags
     * @param sessionId
     * @param pPageable {@link Pageable} Pagination information
     * @return {@link AIP}s corresponding to parameters given.
     */
    Page<AIP> retrieveAIPs(AIPState pState, OffsetDateTime pFrom, OffsetDateTime pTo, List<String> tags,
            String sessionId, Pageable pPageable) throws ModuleException;

    /**
     * Retrieve pages of AIP with files public information filtered according to the parameters
     *
     * @param state cannot be null
     * @param tags can be null
     * @param fromLastUpdateDate can be null
     * @param pageable
     * @return {@link AIP}s corresponding to parameters given.
     */
    Page<AipDataFiles> retrieveAipDataFiles(AIPState state, Set<String> tags, OffsetDateTime fromLastUpdateDate,
            Pageable pageable);

    /**
     * Retrieve the public files metadata associated to an aip
     * @param pIpId
     * @return the files metadata
     * @throws EntityNotFoundException
     */
    Set<OAISDataObject> retrieveAIPFiles(UniformResourceName pIpId) throws ModuleException;

    /**
     * Retrieve storage data files metadata associated to an aip
     * @param pIpId
     * @return the files metadata
     * @throws EntityNotFoundException
     */
    Set<StorageDataFile> retrieveAIPDataFiles(UniformResourceName pIpId) throws ModuleException;

    /**
     * Retrieve the versions of an aip
     * @param pIpId
     * @return the aip versions ip ids
     */
    List<String> retrieveAIPVersionHistory(UniformResourceName pIpId);

    /**
     * Retrieve the input stream towards the desired file.
     * @param pAipId
     * @param pChecksum
     * @return the input stream to the file and its metadata, null if the file is not stored online or in cache
     * @throw EntityNotFoundException if the request {@link StorageDataFile} does not exists.
     */
    Pair<StorageDataFile, InputStream> getAIPDataFile(String pAipId, String pChecksum)
            throws ModuleException, IOException;

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
     * Update PDI and descriptive information of an aip according to updated. To add/remove ContentInformation,
     * storeAndCreate a
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
     * Update PDI and descriptive information of an aip according to updated. To add/remove ContentInformation,
     * storeAndCreate a
     * new aip with a different version and use storeAndCreate method.
     * @param ipId information package identifier of the aip
     * @param updated object containing changes
     * @param updateMessage the message saved inside the AIP
     * @return aip stored into the system after changes have been propagated
     * @throws EntityNotFoundException if no aip with ipId as identifier can be found
     * @throws EntityInconsistentIdentifierException if ipId and updated ipId are different
     * @throws EntityOperationForbiddenException if aip in the system is not in the right state
     */
    AIP updateAip(String ipId, AIP updated, String updateMessage)
            throws EntityNotFoundException, EntityInconsistentIdentifierException, EntityOperationForbiddenException;

    /**
     * Updates all AIP metadta to update.
     */
    void updateAipMetadata();

    /**
     * Remove an aip from the system. Its file are deleted if and only if no other aip point to them.
     * @return not suppressible files because they are in state
     *         {@link fr.cnes.regards.modules.storage.domain.database.DataFileState#PENDING}
     */
    Set<StorageDataFile> deleteAip(String ipId) throws ModuleException;

    /**
     * Remove an aip from the system. Its file are deleted if and only if no other aip point to them.
     * @return not suppressible files because they are in state
     *         {@link fr.cnes.regards.modules.storage.domain.database.DataFileState#PENDING}
     */
    Set<StorageDataFile> deleteAip(AIP aip) throws ModuleException;

    /**
     * Schedule deletion of datafiles marked for deletion
     */
    void doDelete();

    /**
     * Remove {@link AIP}s associated the given sip, through its ip id
     */
    Set<StorageDataFile> deleteAipFromSip(UniformResourceName sipId) throws ModuleException;

    /**
     * Add tags to the specified aip, through its ip id
     * @param ipId
     * @param tagsToAdd
     */
    void addTags(String ipId, Set<String> tagsToAdd)
            throws EntityNotFoundException, EntityInconsistentIdentifierException, EntityOperationForbiddenException;

    /**
     * Add tags to the specified AIP entity, using the entity
     * @param toUpdate
     * @param tagsToAdd
     * @throws EntityNotFoundException
     * @throws EntityInconsistentIdentifierException
     * @throws EntityOperationForbiddenException
     */
    void addTags(AIP toUpdate, Set<String> tagsToAdd)
            throws EntityNotFoundException, EntityInconsistentIdentifierException, EntityOperationForbiddenException;

    /**
     * Add tags to several AIPs, using query filters
     * This method returns before tags are updated, as this method just launch a job
     * @param filters REST query
     */
    void addTagsByQuery(AddAIPTagsFilters filters);

    /**
     * Removes tags from a specified aip, through its ip id
     * @param ipId
     * @param tagsToRemove
     */
    void removeTags(String ipId, Set<String> tagsToRemove)
            throws EntityNotFoundException, EntityInconsistentIdentifierException, EntityOperationForbiddenException;

    /**
     * Remove a list of tags to the provided AIP
     * @param toUpdate
     * @param tagsToRemove
     * @throws EntityNotFoundException
     * @throws EntityInconsistentIdentifierException
     * @throws EntityOperationForbiddenException
     */
    void removeTags(AIP toUpdate, Set<String> tagsToRemove)
            throws EntityNotFoundException, EntityInconsistentIdentifierException, EntityOperationForbiddenException;

    /**
     * Remove a set of tags from several AIPS, using query filters
     * This method returns before tags are updated, as this method just launch a job
     * @param request the request object
     */
    void removeTagsByQuery(RemoveAIPTagsFilters request);

    /**
     * Retrieve one {@link AIPSession} by id.
     * This method returns the AIPSession store in the DAO, so there is no stats computed. See getSessionWithStats for
     * sesion for stats
     * @param sessionId {@link String}
     * @param createIfNotExists if true, the session with sessionId is created is it does not exists.
     * @return {@link AIPSession}
     */
    AIPSession getSession(String sessionId, Boolean createIfNotExists);

    /**
     * Retrieve one {@link AIPSession} by id, and compute its stats.
     * @param sessionId {@link String}
     * @return {@link AIPSession}
     */
    AIPSession getSessionWithStats(String sessionId);

    /**
     * Retrieve all {@link AIPSession} that match provided filters
     * @return {@link AIPSession}s
     */
    Page<AIPSession> searchSessions(String id, OffsetDateTime from, OffsetDateTime to, Pageable pageable);

    /**
     * Delete several {@link AIP}s using query filters
     * This method returns before AIPs are deleted, as this method just launch a job
     */
    void deleteAIPsByQuery(AIPQueryFilters request);

    List<RejectedSip> deleteAipFromSips(Set<String> sipIds) throws ModuleException;

    /**
     * Retrieve all tags used by a set of AIPS, using query filters or a list of AIP id
     * @param filters REST query
     */
    List<String> retrieveAIPTagsByQuery(AIPQueryFilters filters);

    ///////////////////////////////////////////////////////////////////////////////////////////
    ////////////////// These methods should only be called by IAIPServices
    ///////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Schedule new {@link UpdateDataFilesJob}s for all {@link StorageDataFile} of AIP metadata files given
     * and set there state to STORING_METADATA.
     * @param metadataToStore List of {@link StorageDataFile} of new AIP metadata files mapped to old ones.
     */
    void scheduleStorageMetadata(Set<StorageDataFile> metadataToStore);

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
    Set<StorageDataFile> prepareNotFullyStored();

    /**
     * Handle physical deletion of AIPs for each entity in state DELETED and associated to no other
     * StorageDataFile. This state is reached when all locations of all DataObject are deleted for an AIP metadata.
     */
    void removeDeletedAIPMetadatas();
}
