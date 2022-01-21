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
package fr.cnes.regards.modules.storage.domain.flow;

import java.util.Collection;
import java.util.Set;

import javax.validation.Valid;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.storage.domain.dto.request.FileStorageRequestDTO;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storage.domain.event.FileRequestsGroupEvent;

/**
 * Flow message to request a new file storage.<br/>
 * See {@link FileRequestsGroupEvent} for asynchronous responses when request is finished.<br/>
 * See {@link FileReferenceEvent} for asynchronous responses when a file handled.<br/>
 *
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class StorageFlowItem implements ISubscribable {

    /**
     * Maximum number of Request per flow item
     */
    public static final int MAX_REQUEST_PER_GROUP = 500;

    /**
     * Information about files to store
     */
    @Valid
    private final Set<FileStorageRequestDTO> files = Sets.newHashSet();

    /**
     * Request business identifier
     */
    private String groupId;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Set<FileStorageRequestDTO> getFiles() {
        return files;
    }

    /**
     * Build a storage request message for one file
     * @param file
     * @param groupId
     * @return {@link StorageFlowItem}
     */
    public static StorageFlowItem build(FileStorageRequestDTO file, String groupId) {
        StorageFlowItem item = new StorageFlowItem();
        item.files.add(file);
        item.groupId = groupId;
        return item;
    }

    /**
     * Build a storage request message fr many files
     * @param files
     * @param groupId
     * @return {@link StorageFlowItem}
     */
    public static StorageFlowItem build(Collection<FileStorageRequestDTO> files, String groupId) {
        StorageFlowItem item = new StorageFlowItem();
        item.files.addAll(files);
        item.groupId = groupId;
        return item;
    }

    @Override
    public String toString() {
        return "FileStorageFlowItem [" + (files != null ? "files=" + files + ", " : "")
                + (groupId != null ? "groupId=" + groupId : "") + "]";
    }

}
