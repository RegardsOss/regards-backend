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
package fr.cnes.regards.modules.filecatalog.amqp.output;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.fileaccess.dto.request.FileGroupRequestStatus;
import fr.cnes.regards.modules.fileaccess.dto.request.FileRequestGroupDto;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesDeletionEvent;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.input.FilesStorageRequestEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bus message response of a request like {@link FilesStorageRequestEvent},
 * {@link FilesReferenceEvent} or {@link FilesDeletionEvent}.<br/>
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
public class FileRequestsGroupEvent extends FileRequestGroupDto implements ISubscribable {

    private String message;

    public FileRequestsGroupEvent(String groupId,
                                  FileRequestType type,
                                  FileGroupRequestStatus state,
                                  Set<RequestResultInfoDto> errors,
                                  Set<RequestResultInfoDto> success,
                                  String message) {
        super(groupId, type, state, errors, success);
        this.message = message;
    }

    /**
     * Build a message event with the given state
     *
     * @return {@link FileRequestsGroupEvent}
     */
    public static FileRequestsGroupEvent build(String groupId,
                                               FileRequestType type,
                                               FileGroupRequestStatus state,
                                               Collection<RequestResultInfoDto> success) {

        Set<RequestResultInfoDto> successesToAdd = (success.stream()
                                                           .map(s -> RequestResultInfoDto.build(s.getGroupId(),
                                                                                                s.getRequestChecksum(),
                                                                                                s.getRequestStorage(),
                                                                                                s.getRequestStorePath(),
                                                                                                s.getRequestOwners(),
                                                                                                s.getResultFile(),
                                                                                                s.getErrorCause()))
                                                           .collect(Collectors.toSet()));
        return new FileRequestsGroupEvent(groupId, type, state, new HashSet<>(), successesToAdd, null);
    }

    /**
     * Build an error message event with the given {@link ErrorFile}s
     *
     * @return {@link FileRequestsGroupEvent}
     */
    public static FileRequestsGroupEvent buildError(String groupId,
                                                    FileRequestType type,
                                                    Collection<RequestResultInfoDto> success,
                                                    Collection<RequestResultInfoDto> errors) {
        return new FileRequestsGroupEvent(groupId,
                                          type,
                                          FileGroupRequestStatus.ERROR,
                                          new HashSet<>(errors),
                                          new HashSet<>(success),
                                          null);
    }

    public String getMessage() {
        return message;
    }

    public FileRequestsGroupEvent withMessage(String message) {
        this.message = message;
        return this;
    }
}
