/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.storage.domain.dto.request.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storage.domain.event.FileRequestsGroupEvent;

/**
 * Flow message to request file(s) reference deletion.<br/>
 * A deletion request is always a success as the only action is to remove the requesting owner to the file(s)<br/>
 * When a file does not belongs to any owner anymore, then a deletion request is made for stored files (ONLINE and NEARLINE).<br/>
 * <br/>
 * See {@link FileRequestsGroupEvent} for asynchronous responses when request is finished.<br/>
 * See {@link FileReferenceEvent} for asynchronous responses when a file handled.<br/>
 *
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class DeletionFlowItem implements ISubscribable {

    /**
     * Maximum number of Request per flow item
     */
    public static final int MAX_REQUEST_PER_GROUP = 100;

    /**
     * Lock used to avoid running multiple deletion at the same time.
     */
    public static final String DELETION_LOCK = "deletion-lock";

    /**
     * Files to delete information
     */
    private final Set<FileDeletionRequestDTO> files = Sets.newHashSet();

    /**
     * Business request identifier
     */
    private String groupId;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public Set<FileDeletionRequestDTO> getFiles() {
        return files;
    }

    /**
     * Build a deletion request for one {@link FileDeletionRequestDTO} file.
     * @param file {@link FileDeletionRequestDTO} to remove information
     * @param groupId business request identifier
     * @return {@link DeletionFlowItem}
     */
    public static DeletionFlowItem build(FileDeletionRequestDTO file, String groupId) {
        DeletionFlowItem item = new DeletionFlowItem();
        item.files.add(file);
        item.groupId = groupId;
        return item;
    }

    /**
     * Build a deletion request for many {@link FileDeletionRequestDTO} files.
     * @param files {@link FileDeletionRequestDTO}s to remove information
     * @param groupId business request identifier
     * @return {@link DeletionFlowItem}
     */
    public static DeletionFlowItem build(Collection<FileDeletionRequestDTO> files, String groupId) {
        DeletionFlowItem item = new DeletionFlowItem();
        item.files.addAll(files);
        item.groupId = groupId;
        return item;
    }

    @Override
    public String toString() {
        return "DeleteFileRefFlowItem [" + (files != null ? "files=" + files + ", " : "")
                + (groupId != null ? "groupId=" + groupId : "") + "]";
    }

}
