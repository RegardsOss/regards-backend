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
package fr.cnes.regards.modules.filecatalog.amqp.input;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileRequestsGroupEvent;
import fr.cnes.regards.modules.filecatalog.dto.files.FilesStorageRequestDto;
import fr.cnes.regards.modules.filecatalog.dto.request.FileStorageRequestDto;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Event to request a new file storage.<br/>
 * See {@link FileRequestsGroupEvent} for asynchronous responses when request is finished.<br/>
 * See {@link FileReferenceEvent} for asynchronous responses when a file handled.<br/>
 *
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class FilesStorageRequestEvent extends FilesStorageRequestDto implements ISubscribable {

    /**
     * Maximum number of Request per event
     */
    public static final int MAX_REQUEST_PER_GROUP = 500;

    public FilesStorageRequestEvent() {
        super();
    }

    public FilesStorageRequestEvent(Set<FileStorageRequestDto> files, String groupId) {
        super(groupId, files);
    }

    public FilesStorageRequestEvent(Collection<FileStorageRequestDto> files, String groupId) {
        super(groupId, new HashSet<>(files));
    }

    public FilesStorageRequestEvent(FileStorageRequestDto file, String groupId) {
        super(groupId, Stream.of(file).collect(Collectors.toSet()));
    }

}
