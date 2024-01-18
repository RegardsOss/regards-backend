/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.client;

import fr.cnes.regards.modules.fileaccess.amqp.input.*;
import fr.cnes.regards.modules.fileaccess.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import fr.cnes.regards.modules.filecatalog.client.listener.IStorageFileListener;
import fr.cnes.regards.modules.filecatalog.client.listener.IStorageRequestListener;
import fr.cnes.regards.modules.fileaccess.dto.request.FileCopyDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileDeletionDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileReferenceRequestDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestDto;

import java.util.Collection;

/**
 * Client interface for requesting the file storage service
 * <p>
 * Client requests are done asynchronously.
 * To listen to the feedback messages, you have to implement your own message handler listening to {@link FileReferenceEvent}.
 * Be sure to check that the message is intended for you by validating the owner.
 * Look at {@link IStorageRequestListener} to adapt your behavior after global request finished status.
 * Look at {@link IStorageFileListener} to adapt your behavior after each file modification done.
 */
public interface IStorageClient {

    /**
     * Requests storage of a file from a local accessible URL to a destination storage defined
     * by Plugin Configuration businessId of StorageLocation plugin.
     * <br/>
     *
     * @param file {@link FileStorageRequestDto} information about file to store
     * @return {@link RequestInfo} containing a unique request id. This request id can
     * be used to identify responses in {@link IStorageRequestListener} implementation.
     */
    RequestInfo store(FileStorageRequestDto file);

    /**
     * Request storage of a collection of files from a local accessible URL to a destination storage defined
     * by Plugin Configuration businessId of StorageLocation plugin.
     *
     * @param files {@link FileStorageRequestDto} information about files to store
     * @return {@link RequestInfo}s containing a unique request id for each group of requests. a group can contains
     * {@link FilesStorageRequestEvent#MAX_REQUEST_PER_GROUP} at most. Those request info can be used to identify responses
     * in {@link IStorageRequestListener} implementation.
     */
    Collection<RequestInfo> store(Collection<FileStorageRequestDto> files);

    /**
     * Retry all registered request in error associated to the given {@link RequestInfo}
     *
     * @param requestInfo containing a unique request id.
     */
    void storeRetry(RequestInfo requestInfo);

    /**
     * Retry all registered request in error associated to the given owners.
     *
     * @param owners containing a unique request id.
     */
    void storeRetry(Collection<String> owners);

    /**
     * Retry all registered request in error associated to the given {@link RequestInfo}
     *
     * @param requestInfo containing a unique request id.
     */
    void availabilityRetry(RequestInfo requestInfo);

    /**
     * Requests to reference a file at a given storage location. With this request, file is not moved, there are only referenced.
     * <br/>
     *
     * @param file {@link FileReferenceRequestDto} information about files to reference
     * @return {@link RequestInfo} containing a unique request id. This request id can
     * be used to identify responses in {@link IStorageRequestListener} and {@link IStorageFileListener} implementation.
     */
    RequestInfo reference(FileReferenceRequestDto file);

    /**
     * Request to reference a collection of files at given storage locations. With this request, files is not moved,
     * there are only referenced.
     * <br/>
     *
     * @param files {@link FileReferenceRequestDto} information about files to reference
     * @return {@link RequestInfo}s containing a unique request id for each group of requests. a group can contains
     * {@link FilesReferenceEvent#MAX_REQUEST_PER_GROUP} at most. Those request info can be used to identify responses
     * in {@link IStorageRequestListener} implementation.
     */
    Collection<RequestInfo> reference(Collection<FileReferenceRequestDto> files);

    /**
     * Requests the deletion of the file identified by its checksum on the specified storage.<br/>
     * It is necessary to specify the owner as the file can be owned by several owners (multiple references).<br/>
     * As a result, the file will be really deleted if and only if no other owner remains!
     *
     * @param file {@link FileDeletionDto} information about file to delete
     * @return {@link RequestInfo} containing a unique request id. This request id can
     * be used to identify responses in {@link IStorageRequestListener} and {@link IStorageFileListener} implementation.
     */
    RequestInfo delete(FileDeletionDto file);

    /**
     * Requests the deletion of a collection of  files identified by there checksum on the specified storage.<br/>
     * It is necessary to specify the owner as the file can be owned by several owners (multiple references).<br/>
     * As a result, the file will be really deleted if and only if no other owner remains!
     *
     * @param files {@link FileDeletionDto}s information about files to delete
     * @return {@link RequestInfo}s containing a unique request id for each group of requests. a group can contains
     * {@link FilesDeletionEvent#MAX_REQUEST_PER_GROUP} at most. Those request info can be used to identify responses
     * in {@link IStorageRequestListener} implementation.
     */
    Collection<RequestInfo> delete(Collection<FileDeletionDto> files);

    /**
     * Requests the copy of a file identified is checksum to a specified storage.<br/>
     * New copied files will be referenced with the same owners as the original files.<br/>
     *
     * @param file {@link FileCopyDto} information about file to copy
     * @return {@link RequestInfo} containing a unique request id. This request id can
     * be used to identify responses in {@link IStorageRequestListener} and {@link IStorageFileListener} implementation.
     */
    RequestInfo copy(FileCopyDto file);

    /**
     * Requests the copy of a collection of files identified by there checksum to a specified storage.<br/>
     * New copied files will be referenced with the same owners as the original files.<br/>
     *
     * @param files {@link FileCopyDto} information about files to copy
     * @return {@link RequestInfo}s containing a unique request id for each group of requests. a group can contains
     * {@link FilesCopyEvent#MAX_REQUEST_PER_GROUP} at most. Those request info can be used to identify responses
     * in {@link IStorageRequestListener} implementation.
     */
    Collection<RequestInfo> copy(Collection<FileCopyDto> files);

    /**
     * Requests that files identified by their checksums be put online so that they can be downloaded by a third party component.
     *
     * @param checksums         list of file checksums
     * @param availabilityHours duration (in hours) until which the files must be available in the cache
     *                          (after this duration, the system could proceed to a possible cleaning of its cache, only
     *                          offline files are concerned!)
     * @return {@link RequestInfo}s containing a unique request id for each group of requests. a group can contains
     * {@link FilesRestorationRequestEvent#MAX_REQUEST_PER_GROUP} at most. Those request info can be used to identify responses
     * in {@link IStorageRequestListener} implementation.
     */
    Collection<RequestInfo> makeAvailable(Collection<String> checksums, int availabilityHours);

    /**
     * Submit a cancel request to storage microservice. The cancel request remove all requests associated to the given request group ids.
     *
     * @param requestGroups {@link String} list of group ids to cancel requests from.
     */
    void cancelRequests(Collection<String> requestGroups);
}
