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
package fr.cnes.regards.modules.fileaccess.amqp.input;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.fileaccess.dto.files.FilesStorageRequestDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestDto;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Event to request a new file storage to the file access microservice.<br/>
 * This request will be processed once this event is received without further filtering.
 *
 * @author Thibaud Michaudel
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class FilesStorageRequestReadyToProcessEvent extends FilesStorageRequestDto implements ISubscribable {

    /**
     * Maximum number of Request per event
     */
    public static final int MAX_REQUEST_PER_GROUP = 500;

    public FilesStorageRequestReadyToProcessEvent() {
        super();
    }

    public FilesStorageRequestReadyToProcessEvent(Set<FileStorageRequestDto> files, String groupId) {
        super(groupId, files);
    }

    public FilesStorageRequestReadyToProcessEvent(Collection<FileStorageRequestDto> files, String groupId) {
        super(groupId, new HashSet<>(files));
    }

    public FilesStorageRequestReadyToProcessEvent(FileStorageRequestDto file, String groupId) {
        super(groupId, Stream.of(file).collect(Collectors.toSet()));
    }

}
