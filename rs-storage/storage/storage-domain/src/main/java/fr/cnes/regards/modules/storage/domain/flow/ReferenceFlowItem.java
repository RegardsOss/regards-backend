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
import java.util.HashMap;
import java.util.Set;

import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.storage.domain.dto.request.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storage.domain.event.FileReferenceEvent;
import fr.cnes.regards.modules.storage.domain.event.FileRequestsGroupEvent;

/**
 * Flow message to request a new file reference.<br/>
 * See {@link FileRequestsGroupEvent} for asynchronous responses when request is finished.<br/>
 * See {@link FileReferenceEvent} for asynchronous responses when a file handled.<br/>
 *
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class ReferenceFlowItem implements ISubscribable {

    /**
     * Maximum number of Request per flow item
     */
    public static final int MAX_REQUEST_PER_GROUP = 100;

    /**
     * Information about files to reference.
     */
    private final Set<FileReferenceRequestDTO> files = Sets.newHashSet();

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

    public Set<FileReferenceRequestDTO> getFiles() {
        return files;
    }

    /**
     * Build a file reference request event for one file
     * @param file {@link FileReferenceRequestDTO} file to reference information
     * @param groupId business request identifier to identify request in asynchronous response messages {@link FileRequestsGroupEvent}
     * @return {@link ReferenceFlowItem}
     */
    public static ReferenceFlowItem build(FileReferenceRequestDTO file, String groupId) {
        ReferenceFlowItem item = new ReferenceFlowItem();
        item.files.add(file);
        item.groupId = groupId;
        return item;
    }

    /**
     * Build a file reference request event for a collection of files
     * @param files  {@link FileReferenceRequestDTO} files to reference information
     * @param groupId business request identifier to identify request in asynchronous response messages {@link FileRequestsGroupEvent}
     * @return {@link ReferenceFlowItem}
     */
    public static ReferenceFlowItem build(Collection<FileReferenceRequestDTO> files, String groupId) {
        ReferenceFlowItem item = new ReferenceFlowItem();
        item.files.addAll(files);
        item.groupId = groupId;
        return item;
    }

    @Override
    public String toString() {
        return "FileReferenceFlowItem [" + (files != null ? "files=" + files + ", " : "")
                + (groupId != null ? "groupId=" + groupId : "") + "]";
    }

    /**
     *
     */
    public Errors validate(Validator validator) {
        Errors errors;
        if (files.size() > MAX_REQUEST_PER_GROUP) {
            errors = new MapBindingResult(new HashMap<>(), this.getClass().getName());
            errors.reject("FileReferenceRequests",
                          String.format("Number of reference requests (%d) for group %s exceeds maximum limit of %d",
                                        files.size(), groupId, MAX_REQUEST_PER_GROUP));
        } else {
            errors = new MapBindingResult(new HashMap<>(), FileReferenceRequestDTO.class.getName());
            for (FileReferenceRequestDTO file : files) {
                validator.validate(file, errors);
            }
        }
        return errors;
    }

}
