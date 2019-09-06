/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.domain.event;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.storagelight.domain.database.request.group.GroupRequestsInfo;
import fr.cnes.regards.modules.storagelight.domain.dto.request.group.GroupRequestInfoDTO;
import fr.cnes.regards.modules.storagelight.domain.flow.DeletionFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.FlowItemStatus;
import fr.cnes.regards.modules.storagelight.domain.flow.ReferenceFlowItem;
import fr.cnes.regards.modules.storagelight.domain.flow.StorageFlowItem;

/**
 * Bus message response of a request like {@link StorageFlowItem},
 * {@link ReferenceFlowItem} or {@link DeletionFlowItem}.<br/>
 * <br/>
 * FileRequestEventState :<ul>
 * <li> GRANTED : sent when the request is handled.</li>
 * <li> DENIED : sent when the request is refused.</li>
 * <li> DONE / ERROR : sent when all files in the request are handled.</li>
 * <li>
 *
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class FileRequestsGroupEvent implements ISubscribable {

    /**
     * Business request identifier
     */
    private String groupId;

    /**
     * Request status
     */
    private FlowItemStatus state;

    /**
     * Request type
     */
    private FileRequestType type;

    /**
     * Files in error status
     */
    private final Set<GroupRequestInfoDTO> errors = Sets.newHashSet();

    /**
     * Files in error status
     */
    private final Set<GroupRequestInfoDTO> success = Sets.newHashSet();

    private String message;

    /**
     * Build a message event with the given state
     * @param groupId
     * @param type
     * @param state
     * @return {@link FileRequestsGroupEvent}
     */
    public static FileRequestsGroupEvent build(String groupId, FileRequestType type, FlowItemStatus state,
            Collection<GroupRequestsInfo> success) {
        Assert.notNull(groupId, "Request Id is mandatory");
        Assert.notNull(type, "Request type is mandatory");
        Assert.notNull(state, "Request state is mandatory");
        FileRequestsGroupEvent event = new FileRequestsGroupEvent();
        event.groupId = groupId;
        event.state = state;
        event.type = type;
        event.success.addAll(success.stream()
                .map(s -> GroupRequestInfoDTO.build(s.getGroupId(), s.getChecksum(), s.getStorage(),
                                                    s.getFileReference(), s.getErrorCause()))
                .collect(Collectors.toSet()));
        return event;
    }

    public FileRequestsGroupEvent withMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Build an error message event with the given {@link ErrorFile}s
     * @param groupId
     * @param type
     * @param errors
     * @return {@link FileRequestsGroupEvent}
     */
    public static FileRequestsGroupEvent buildError(String groupId, FileRequestType type,
            Collection<GroupRequestsInfo> success, Collection<GroupRequestsInfo> errors) {
        Assert.notNull(groupId, "Request Id is mandatory");
        Assert.notNull(type, "Request type is mandatory");
        FileRequestsGroupEvent event = new FileRequestsGroupEvent();
        event.groupId = groupId;
        event.state = FlowItemStatus.ERROR;
        event.errors.addAll(errors.stream()
                .map(e -> GroupRequestInfoDTO.build(e.getGroupId(), e.getChecksum(), e.getStorage(),
                                                    e.getFileReference(), e.getErrorCause()))
                .collect(Collectors.toSet()));
        event.success.addAll(success.stream()
                .map(s -> GroupRequestInfoDTO.build(s.getGroupId(), s.getChecksum(), s.getStorage(),
                                                    s.getFileReference(), s.getErrorCause()))
                .collect(Collectors.toSet()));
        event.type = type;
        return event;
    }

    public Set<GroupRequestInfoDTO> getErrors() {
        return errors;
    }

    public FileRequestType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getGroupId() {
        return groupId;
    }

    public FlowItemStatus getState() {
        return state;
    }

    public Set<GroupRequestInfoDTO> getSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "FileRequestEvent [" + (groupId != null ? "groupId=" + groupId + ", " : "")
                + (state != null ? "state=" + state + ", " : "") + (type != null ? "type=" + type + ", " : "")
                + (errors != null ? "errors=" + errors : "") + "]";
    }

}
