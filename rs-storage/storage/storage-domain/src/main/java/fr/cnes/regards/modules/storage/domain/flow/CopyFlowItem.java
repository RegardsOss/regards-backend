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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.storage.domain.dto.request.FileCopyRequestDTO;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storage.domain.event.FileRequestsGroupEvent;

import java.util.Collection;
import java.util.Set;

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
public class CopyFlowItem implements ISubscribable {

    public static final int MAX_REQUEST_PER_GROUP = 500;

    /**
     * Files to delete information
     */
    private final Set<FileCopyRequestDTO> files = Sets.newHashSet();

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

    public Set<FileCopyRequestDTO> getFiles() {
        return files;
    }

    /**
     * Build a copy request for one {@link FileCopyRequestDTO} file.
     *
     * @param file    {@link FileCopyRequestDTO} to remove information
     * @param groupId business request identifier
     * @return {@link CopyFlowItem}
     */
    public static CopyFlowItem build(FileCopyRequestDTO file, String groupId) {
        CopyFlowItem item = new CopyFlowItem();
        item.files.add(file);
        item.groupId = groupId;
        return item;
    }

    /**
     * Build a copy request for many {@link FileCopyRequestDTO} files.
     *
     * @param files   {@link FileCopyRequestDTO}s to remove information
     * @param groupId business request identifier
     * @return {@link CopyFlowItem}
     */
    public static CopyFlowItem build(Collection<FileCopyRequestDTO> files, String groupId) {
        CopyFlowItem item = new CopyFlowItem();
        item.files.addAll(files);
        item.groupId = groupId;
        return item;
    }

    @Override
    public String toString() {
        return "DeleteFileRefFlowItem [" + (files != null ? "files=" + files + ", " : "") + (groupId != null ?
            "groupId=" + groupId :
            "") + "]";
    }

}
